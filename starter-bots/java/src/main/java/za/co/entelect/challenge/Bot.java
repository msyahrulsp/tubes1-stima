package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;
import static java.lang.Math.max;

public class Bot {

    private static final int minSpeed = 0;
    private static final int speedState1 = 3;
    private static final int initialSpeed = 5;
    private static final int speedState2 = 6;
    private static final int speedState3 = 8;
    private static final int maxSpeed = 9;
    private static final int boostSpeed = 15;

    private GameState gameState;
    private Car opponent;
    private Car myCar;
    private Boolean right;
    private Boolean left;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    private final static Command NOTHING = new DoNothingCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    // Command Bot diexecute parallel
    public Bot(GameState gameState) {
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;
        this.right = this.myCar.position.lane + 1 <= 4;
        this.left = this.myCar.position.lane - 1 >= 1;
    }

    public Command run() {
        Command action;
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed);
        List<Object> blocks2 = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, getNextSpeed(myCar.speed));
        List<Object> blocksBoost = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, 15);

        // Prioritasin repair abis Hit Wall atau Cyber Truck
        if (myCar.state == State.HIT_CYBER_TRUCK || myCar.state == State.HIT_WALL) {
            return FIX;
        }

        // Damage harus selalu minimal 2 supaya bisa pake boost
        if (myCar.damage >= 1) {
            return FIX;
        }

        // Safecase Stuck
        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        // Pake Boost cuman kalau kosong banget
        if (!laneHaveObstacle(blocksBoost) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting && myCar.damage == 0) {
            return BOOST;
        }

        // Accelerate ketika next speed state kosong obstacle
        if (!laneHaveObstacle(blocks2) && myCar.speed < maxSpeed) {
            return ACCELERATE;
        }

        // Avoid Logic buat kalau kena obstacle di speed yang sekarang
        if (laneHaveObstacle(blocks)) {
            action = avoidMove(blocks);
            if (action != NOTHING) {
                return action;
            }
            int dec = 0;
            if (myCar.speed == boostSpeed) {
                dec = 6;
            } else if (myCar.speed == maxSpeed) {
                dec = 1;
            } else if (myCar.speed == speedState3) {
                dec = 2;
            } else if (myCar.speed == speedState2) {
                dec = 3;
            }
            List<Object> decBlocks = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed - dec);
            if (!laneHaveObstacle(decBlocks)) {
                return DECELERATE;
            }
        }

        // Avoid Logic buat kalau kena obstacle di next speed state (exluding boost speed) 
        if (laneHaveObstacle(blocks2)) {
            action = avoidMove(blocks2);
            if (action != NOTHING) {
                return action;
            }
            int dec = 0;
            if (myCar.speed == boostSpeed) {
                dec = 6;
            } else if (myCar.speed == maxSpeed) {
                dec = 1;
            } else if (myCar.speed == speedState3) {
                dec = 2;
            } else if (myCar.speed == speedState2) {
                dec = 3;
            }
            List<Object> decBlocks = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed - dec);
            if (!laneHaveObstacle(decBlocks)) {
                return DECELERATE;
            }
        }

        // Menggunakan EMP
        if (validEMP()) {
            return EMP;
        }

        // Pake Boost cuman kalau kosong banget
        if (!laneHaveObstacle(blocksBoost) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting && myCar.damage == 0) {
            return BOOST;
        }

        // Summon Cyber Truck boi
        if (validTweet()) {
            int block = opponent.position.block + opponent.speed + 1;
            int lane = opponent.position.lane;
            if (opponent.state == State.USED_BOOST) {
                block = opponent.position.block + boostSpeed + 1;
            } else if (opponent.state == State.ACCELERATING) {
                if (opponent.speed == minSpeed) {
                    block = opponent.position.block + speedState1 + 1;
                } else if (opponent.speed == speedState1) {
                    block = opponent.position.block + speedState2 + 1;
                } else if (opponent.speed == speedState2) {
                    block = opponent.position.block + speedState3 + 1;
                }
            }
            return new TweetCommand(lane, block);
        }

        // Ambil Power Up
        // Priority : EMP > Tweet > Boost > Lizard > Oil (Hoki2an)
        if (!hasPowerUp(PowerUps.EMP, myCar.powerups)) {
            return prioPower(Terrain.EMP);
        } else if (!hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            return prioPower(Terrain.TWEET_POWER);
        } else if (!hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return prioPower(Terrain.BOOST);
        } else if (!hasPowerUp(PowerUps.LIZARD, myCar.powerups) && safeLanding(blocks) ) {
            return prioPower(Terrain.LIZARD);
        }

        // Summon OIL
        if (validOIL()) {
            return OIL;
        }

        // Default return
        return ACCELERATE;
    }

    private Command avoidMove(List<Object> blocks) {
        // Car berada di lane 2 atau 3
        if (right && left) {
            List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
            List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
            if (!laneHaveObstacle(rightBlocks) && (laneHavePower(rightBlocks, Terrain.BOOST) || laneHavePower(rightBlocks, Terrain.LIZARD))) {
                return TURN_RIGHT;
            }
            if (!laneHaveObstacle(leftBlocks) && (laneHavePower(leftBlocks, Terrain.BOOST) || laneHavePower(leftBlocks, Terrain.LIZARD))) {
                return TURN_LEFT;
            }
            if (!laneHaveObstacle(rightBlocks)) {
                return TURN_RIGHT;
            }
            if (!laneHaveObstacle(leftBlocks)) {
                return TURN_LEFT;
            }
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && safeLanding(blocks) ) {
                return LIZARD;
            }

            if (laneHavePower(blocks, Terrain.BOOST) || laneHavePower(blocks, Terrain.LIZARD)) {
                return ACCELERATE;
            }

            if (laneHavePower(rightBlocks, Terrain.BOOST) || laneHavePower(rightBlocks, Terrain.LIZARD)) {
                return TURN_RIGHT;
            }

            if (laneHavePower(leftBlocks, Terrain.BOOST) || laneHavePower(leftBlocks, Terrain.LIZARD)) {
                return TURN_LEFT;
            }
        } else {
            // Car ada di lane < 4
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(rightBlocks)) {
                    return TURN_RIGHT;
                }

                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && safeLanding(blocks) ) {
                    return LIZARD;
                }
                
                if (laneHavePower(blocks, Terrain.BOOST) || laneHavePower(blocks, Terrain.LIZARD)) {
                    return ACCELERATE;
                }
    
                if (laneHavePower(rightBlocks, Terrain.BOOST) || laneHavePower(rightBlocks, Terrain.LIZARD)) {
                    return TURN_RIGHT;
                }
            }
            // Car ada di lane > 1
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(leftBlocks)) {
                    return TURN_LEFT;
                }
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && safeLanding(blocks) ) {
                    return LIZARD;
                }
                
                if (laneHavePower(blocks, Terrain.BOOST) || laneHavePower(blocks, Terrain.LIZARD)) {
                    return ACCELERATE;
                }

                if (laneHavePower(leftBlocks, Terrain.BOOST) || laneHavePower(leftBlocks, Terrain.LIZARD)) {
                    return TURN_LEFT;
                }
            }
        }
        return NOTHING;
    }

    private Command prioPower(Terrain power) {
        // Car ada di lane 2 atau 3
        if (right && left) {
            List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
            List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
            if (!laneHaveObstacle(rightBlocks) && laneHavePower(rightBlocks, power)) {
                return TURN_RIGHT;
            }
            if (!laneHaveObstacle(leftBlocks) && laneHavePower(leftBlocks, power)) {
                return TURN_LEFT;
            }
        } else {
            // Car ada di lane < 4
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(rightBlocks) && laneHavePower(rightBlocks, power)) {
                    return TURN_RIGHT;
                }
            }
            // Cara da di lane > 1
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(leftBlocks) && laneHavePower(leftBlocks, power)) {
                    return TURN_LEFT;
                }
            }
        }
        return ACCELERATE;
    }

    // Cek opponent ada di lane yang valid buat emp
    private Boolean inAdjacentLane() {
        return (
            opponent.position.lane <= myCar.position.lane + 1 && opponent.position.lane >= myCar.position.lane - 1
        );
    }

    // Cek powerup di invent
    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    // Cek lane punya certain powerup atau gk
    private Boolean laneHavePower(List<Object> blocks, Terrain terrain) {
        return (
            blocks.contains(terrain)
        );
    }

    // Cek bisa shoot emp atau gk
    private Boolean validEMP() {
        return (
            myCar.position.block < opponent.position.block
            && inAdjacentLane()
            && hasPowerUp(PowerUps.EMP, myCar.powerups)
        );
    }

    private Boolean validTweet() {
        return (
            hasPowerUp(PowerUps.TWEET, myCar.powerups)
            && opponent.speed >= speedState1
        );
    }

    private Boolean laneHaveObstacle(List<Object> blocks) {
        return (
            blocks.contains(Terrain.MUD)
            || blocks.contains(Terrain.OIL_SPILL)
            || blocks.contains(Terrain.WALL)
            || blocks.contains(Terrain.TWEET_TRUCK)
        );
    }

    // Cek kevalidan kondisi release ~~kracken~~ oil
    private Boolean validOIL() {
        return (
            opponent.position.block < myCar.position.block // Car lain di belakang
            && opponent.position.lane == myCar.position.lane
            && hasPowerUp(PowerUps.OIL, myCar.powerups)
        );
    }

    // Gets block in front
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState, int speed) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            if (laneList[i].tweet) {
                blocks.add(Terrain.TWEET_TRUCK);
            } else {
                blocks.add(laneList[i].terrain);
            }

        }
        return blocks;
    }
  
    // Jika menggunakan boost lizard untuk melompati obstacle
    // pastikan terlebih dahulu aman landingnya
    private boolean safeLanding(List<Object> laneList){
        if (laneList.get(laneList.size() - 1) == Terrain.MUD){
            return false;
        } else if (laneList.get(laneList.size() - 1) == Terrain.WALL){
            return false;
        } else if (laneList.get(laneList.size() - 1) == Terrain.OIL_SPILL){
            return false;
        } 
        
        return true;
    }
  
    // Pengecekan berapa speed selanjutnya
    private int getNextSpeed(int currentSpeed) {
        if (currentSpeed == initialSpeed) {
            return speedState1;
        } else if (currentSpeed == speedState1){
            return speedState2;
        } else if (currentSpeed == speedState2){
            return speedState3;
        } else if (currentSpeed == speedState3){
            return maxSpeed;
        } else if (currentSpeed == maxSpeed){
            return maxSpeed;
        }  else { // speed 0
            return 3;
        }
    }
}