package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

public class Bot {

    private static final int maxSpeed = 9;
    private static final int boostSpeed = 15;

    private GameState gameState;
    private Car opponent;
    private Car myCar;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    // private final static Command TWEET = new TweetCommand(lane, block); getpostmusuh

    // Double Turn 1 Turn gk bisa
    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    // Command Bot diexecute parallel
    public Bot(GameState gameState) {
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;
    }

    public Command run() {
        Boolean right = myCar.position.lane + 1 <= 4;
        Boolean left = myCar.position.lane - 1 >= 1;
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed);

        // Prioritasin repair & boost
        if (myCar.state == State.HIT_CYBER_TRUCK || myCar.state == State.HIT_WALL) {
            return FIX;
        }

        // Boost biar 15 kudu no damage
        if (myCar.damage >= 1 && hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return FIX;
        }

        if (myCar.damage >= 2 && !hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return FIX;
        }

        if (validEMP()) {
            return EMP;
        }

        if (validOIL()) {
            return OIL;
        }

        if (laneHaveObstacle(blocks)) {
            if (right && left) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(rightBlocks) && laneHaveBoost(rightBlocks)) {
                    return TURN_RIGHT;
                }
                if (!laneHaveObstacle(leftBlocks) && laneHaveBoost(rightBlocks)) {
                    return TURN_LEFT;
                }
                if (!laneHaveObstacle(rightBlocks) && laneHavePower(rightBlocks)) {
                    return TURN_RIGHT;
                }
                if (!laneHaveObstacle(leftBlocks) && laneHavePower(rightBlocks)) {
                    return TURN_LEFT;
                }
                if (!laneHaveObstacle(rightBlocks)) {
                    return TURN_RIGHT;
                }
                if (!laneHaveObstacle(leftBlocks)) {
                    return TURN_LEFT;
                }
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            } else {
                if (right) {
                    List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                    if (!laneHaveObstacle(rightBlocks)) {
                        return TURN_RIGHT;
                    }
                }
                if (left) {
                    List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                    if (!laneHaveObstacle(leftBlocks)) {
                        return TURN_LEFT;
                    }
                }
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
        }
        
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return BOOST;
        }

        if (laneHavePower(blocks)) {
            return ACCELERATE;
        }

        if (right) {
            List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
            if (laneHavePower(rightBlocks)) {
                return TURN_RIGHT;
            }
        }
        if (left) {
            List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
            if (laneHavePower(leftBlocks)) {
                return TURN_LEFT;
            }
        }

        return ACCELERATE;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private Boolean laneHavePower(List<Object> blocks) {
        return (
            blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.EMP) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER)
        );
    }

    private Boolean laneHaveBoost(List<Object> blocks) {
        return (
            blocks.contains(Terrain.BOOST)
        );
    }

    private Boolean laveHaveTwitter(List<Object> blocks) {
        return (
            blocks.contains(Terrain.TWEET)
        );
    }

    private Boolean validEMP() {
        return (
            opponent.position.block > myCar.position.block // Car lain di depan
            && opponent.position.lane <= myCar.position.lane + 1 && opponent.position.lane >= myCar.position.lane - 1 // Ada samping kanan / kiri
            && hasPowerUp(PowerUps.EMP, myCar.powerups)
        );
    }

    private Boolean laneHaveObstacle(List<Object> blocks) {
        return (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL));
    }

    // TODO Cek state musuh, gk ada di terrain soalnya
    private Boolean avoidTweet() {
        return true;
    }

    // TODO buat pake lizard incase tabrakan
    private Boolean avoidCollision() {
        return true;
    }

    private Boolean validOIL() {
        return (
            opponent.position.block >= myCar.position.block - 2 && opponent.position.block <= myCar.position.block // Car lain di belakang
            && opponent.position.lane == myCar.position.lane
            && hasPowerUp(PowerUps.OIL, myCar.powerups)
        );
    }
    
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState, int speed) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }
}