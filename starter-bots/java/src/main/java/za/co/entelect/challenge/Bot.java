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
    private List<Integer> directionList = new ArrayList<>();

    private Random random;
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
    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(-1);
        directionList.add(1);
    }

    public Command run() {
        Boolean right = myCar.position.lane + 1 <= 4;
        Boolean left = myCar.position.lane - 1 >= 1;
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, myCar.speed);

        // Prioritasin repair & boost
        if (myCar.state == State.HIT_CYBER_TRUCK || myCar.state == State.HIT_WALL) {
            return FIX;
        }

        if (myCar.damage >= 2 && !hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return FIX;
        }

        if (myCar.damage >= 2 && hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return BOOST;
        }
        
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return BOOST;
        }

        if (validEMP()) {
            return EMP;
        }

        if (checkLanePower(blocks)) {
            return ACCELERATE;
        } else {
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, myCar.speed - 1);
                if (checkLanePower(rightBlocks)) {
                    return TURN_RIGHT;
                }
            }
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, myCar.speed - 1);
                if (checkLanePower(leftBlocks)) {
                    return TURN_LEFT;
                }
            }
        }

        if (hasObstacle(blocks)) {
            if (right) {
                List<Object> rightBlocks = getBlocksInFront(myCar.position.lane + 1, myCar.position.block, myCar.speed - 1);
                if (!hasObstacle(rightBlocks)) {
                    return TURN_RIGHT;
                }
            }
            if (left) {
                List<Object> leftBlocks = getBlocksInFront(myCar.position.lane - 1, myCar.position.block, myCar.speed - 1);
                if (!hasObstacle(leftBlocks)) {
                    return TURN_LEFT;
                }
            }
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            return ACCELERATE;
        }

        if (validOIL()) {
            return OIL;
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

    private Boolean checkLanePower(List<Object> blocks) {
        return (
            blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.EMP) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER)
        );
    }

    private Boolean validEMP() {
        return (
            opponent.position.block > myCar.position.block // Car lain di depan
            && opponent.position.lane <= myCar.position.lane + 1 && opponent.position.lane >= myCar.position.lane - 1 // Ada samping kanan / kiri
            && hasPowerUp(PowerUps.EMP, myCar.powerups)
        );
    }

    private Boolean hasObstacle(List<Object> blocks) {
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
            opponent.position.block < myCar.position.block // Car lain di belakang
            && opponent.position.lane == myCar.position.lane
            && hasPowerUp(PowerUps.OIL, myCar.powerups)
        );
    }
    
    private List<Object> getBlocksInFront(int lane, int block, int speed) {
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
