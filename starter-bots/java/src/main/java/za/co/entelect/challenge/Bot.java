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
        int blocksWeight = getLaneWeight(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed);

        // Prioritasin repair abis Hit Wall atau Cyber Truck
        if (myCar.state == State.HIT_CYBER_TRUCK || myCar.state == State.HIT_WALL) {
            return FIX;
        }

        // Damage harus selalu minimal 2 supaya bisa pake boost
        if (myCar.damage >= 2) {
            return FIX;
        }

        if (laneHaveObstacle(blocks)) {
            action = avoidMove(blocks, blocksWeight);
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
            // TODO Tambahin bobot block
            List<Object> decBlocks = getBlocksInFront(myCar.position.lane, myCar.position.block, this.gameState, myCar.speed - dec);
            if (!laneHaveObstacle(decBlocks)) {
                return DECELERATE;
            }
        }
        
        // Pake Boost cuman kalau kosong banget
        if (!laneHaveObstacle(blocks) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return BOOST;
        }

        if (myCar.speed < maxSpeed) {
            return ACCELERATE;
        }

        if (validEMP()) {
            return EMP;
        }

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
        } else if (!hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
            return prioPower(Terrain.LIZARD);
        }

        if (validOIL()) {
            return OIL;
        }

        return ACCELERATE;
    }

    private Command avoidMove(List<Object> blocks, int weight) {
        if (right && left) {
            List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
            int rightBlocksWeight = getLaneWeight(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
            List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
            int leftBlocksWeight = getLaneWeight(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
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
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            
            if (laneHaveObstacle(blocks) && laneHaveObstacle(rightBlocks) && laneHaveObstacle(leftBlocks)) {
                int max = Math.max(Math.max(weight, rightBlocksWeight), leftBlocksWeight);
                if (weight == max) {
                    return ACCELERATE;
                }
                if (rightBlocksWeight == max) {
                    return TURN_RIGHT;
                }
                if (leftBlocksWeight == max) {
                    return TURN_LEFT;
                }
            }
        } else {
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(rightBlocks)) {
                    return TURN_RIGHT;
                }
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(leftBlocks)) {
                    return TURN_LEFT;
                }
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                    return LIZARD;
                }
            }
        }
        return NOTHING;
    }

    private Command prioPower(Terrain power) {
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
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(rightBlocks) && laneHavePower(rightBlocks, power)) {
                    return TURN_RIGHT;
                }
            }
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, this.gameState, myCar.speed - 1);
                if (!laneHaveObstacle(leftBlocks) && laneHavePower(leftBlocks, power)) {
                    return TURN_LEFT;
                }
            }
        }
        return ACCELERATE;
    }

    private Boolean inAdjacentLane() {
        return (
            opponent.position.lane <= myCar.position.lane + 1 && opponent.position.lane >= myCar.position.lane - 1
        );
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private Boolean laneHavePower(List<Object> blocks, Terrain terrain) {
        return (
            blocks.contains(terrain)
        );
    }

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

    private Boolean validOIL() {
        return (
            opponent.position.block >= myCar.position.block - 2 && opponent.position.block <= myCar.position.block // Car lain di belakang
            && opponent.position.lane == myCar.position.lane
            && hasPowerUp(PowerUps.OIL, myCar.powerups)
        );
    }
    
    private int getLaneWeight(int lane, int block, GameState gameState, int speed) {
        List<Lane[]> map = gameState.lanes;
        int startBlock = map.get(0)[0].position.block;
        int result = 0;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            if (laneList[i].terrain == Terrain.MUD) {
                result -= 2;
            } else if (laneList[i].terrain == Terrain.OIL_SPILL) {
                result -= 2;
            } else if (laneList[i].terrain == Terrain.WALL) {
                result -= 5;
            } else if (laneList[i].terrain == Terrain.TWEET_TRUCK) {
                result -= 5;
            } else if (laneList[i].terrain == Terrain.BOOST) {
                result += 7;
            } else if (laneList[i].terrain == Terrain.LIZARD) {
                result += 5;
            } else if (laneList[i].terrain == Terrain.EMP) {
                result += 5;
            } else if (laneList[i].terrain == Terrain.TWEET_POWER) {
                result += 5;
            } else if (laneList[i].terrain == Terrain.OIL_POWER) {
                result += 2;
            }
        }
        return result;
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

            if (laneList[i].tweet) {
                blocks.add(Terrain.TWEET_TRUCK);
            } else {
                blocks.add(laneList[i].terrain);
            }

        }
        return blocks;
    }
}