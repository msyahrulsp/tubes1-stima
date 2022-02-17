package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

public class Bot {

    
    private List<Command> directionList = new ArrayList<>();

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command DO_NOTHING = new DoNothingCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private static Command TWEET = new TweetCommand(0, 0);
    
    public Bot() {
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;
        
        List<Lane> frontBlocks = getBlocksInFront(gameState, myCar, myCar.speed);
        
        // PRIORITAS UTAMA 
        // Apabila mobil rusak parah sehingga tidak dapat bergerak, perbaiki terlebih dahulu
        if(myCar.damage == 5) {
            return FIX;
        }

        // Apabila kecepatan mobil rendah, maka naikkan kecepatan terlebih dahulu
        if(myCar.speed <= 3) {
            if (myCar.damage >= 1){
                return FIX;
            }
            return ACCELERATE;
        }

        // Pengecekan untuk menghindari obstacle
        if (checkObstacle(frontBlocks)) { 

            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && safeLanding(frontBlocks) ){
                return LIZARD;
            }

            if (isOpponentInFront(myCar.position, opponent.position)) {
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
                    return LIZARD;
                }
            }
            frontBlocks = getBlocksInFront(gameState, myCar, 9);
            if (checkObstacle(frontBlocks)){
                switch (myCar.position.lane){
                    case 1:
                        return TURN_RIGHT;
                    case 2:
                        return TURN_RIGHT;
                    case 3:
                        return TURN_LEFT;
                    case 4:
                        return TURN_LEFT;
                    default:
                        return ACCELERATE;
                }
            }
            
        } else { // Lakukan boost ketika di depan kosong
            frontBlocks = getBlocksInFront(gameState, myCar, getNextSpeed(myCar.speed));
            if (checkObstacle(frontBlocks)) {
                if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
                    return BOOST;
                } else {
                    return ACCELERATE;
                }
            }
        }

        // gunakan powerup boost jika berlebih 
        if (getPowerUpQty(PowerUps.BOOST, myCar.powerups) > 3){
            return BOOST;
        }

        // powerup untuk mengganggu lawan
        
        // menggunakan oil
        if (hasPowerUp(PowerUps.OIL, myCar.powerups)){
            // kalau banyak dipakai untuk mengurangi inventory
            if (getPowerUpQty(PowerUps.OIL, myCar.powerups) > 3){
                return OIL;
            }

            // kalau lawan di lane yang sama dan dekat dengan mobil kita gunakan oil
            if (getOpponentLane(gameState) == myCar.position.lane){
                if (myCar.position.block - getOpponentBlock(gameState) < 8){
                    return OIL;
                }
            }
        }

        // menggunakan EMP
        if (hasPowerUp(PowerUps.EMP, myCar.powerups)){
            if ((myCar.position.lane - getOpponentLane(gameState)) <= 1 || 
            (getOpponentLane(gameState) - myCar.position.lane) <= 1) {
                if (myCar.position.block < getOpponentBlock(gameState));
            }
            return EMP;
        }

        // menggunakan tweet
        if (hasPowerUp(PowerUps.TWEET, myCar.powerups) && (myCar.position.block > getOpponentLane(gameState)) ){
            TWEET = new TweetCommand( getOpponentLane(gameState), getOpponentBlock(gameState)+getOpponentSpeed(gameState)+1);
            return TWEET;
        }

        return ACCELERATE;
        
    }

    // mengembalikan jenis-jenis block sesuai dengan batasan yang diberikan
    private List<Lane> getBlocksInFront(GameState gameState, Car myCar, int increment) {
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<Lane>();
        int startBlock = map.get(0)[0].position.block;
        int endBlock = myCar.position.block - startBlock + increment;
        if (endBlock > 19) {
            endBlock = 19;
        }
    
        Lane[] laneList = map.get(myCar.position.lane - 1);
        for (int i = max(myCar.position.block - startBlock, 0); i <= endBlock; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i]);
        }
        return blocks;
    }

    // cek untuk powerup sesuai dengan jenisnya
    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private int getPowerUpQty(PowerUps powerUpToCheck, PowerUps[] available) {
        int count=0;
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                count++;
            }
        }
        return count;
    }


    private int getOpponentLane (GameState gameState){
        return gameState.opponent.position.lane;
    }

    private int getOpponentBlock (GameState gameState){
        return gameState.opponent.position.block;
    }

    private int getOpponentSpeed (GameState gameState){
        return gameState.opponent.speed;
    }

    private Boolean isOpponentInFront (Position myPosition, Position opponentPosition){
        if (myPosition.lane == opponentPosition.lane){
            if (myPosition.block+1 == opponentPosition.block){
                return true;
            }
        }
        return false;
    }

    // Cek apakah ada obstacle di depan mobil
    private boolean checkObstacle(List<Lane> laneList){
        for (Lane lane: laneList){
            if (lane.terrain == Terrain.MUD){
                return true;
            }
            else if (lane.terrain == Terrain.WALL){
                return true;
            }
            else if (lane.terrain == Terrain.OIL_SPILL){
                return true;
            }
            else if (lane.isOccupiedByCyberTruck){
                return true;
            }
        }
        return false;
    }

    // Jika menggunakan boost lizard untuk melompati obstacle
    // pastikan terlebih dahulu aman landingnya
    private boolean safeLanding(List<Lane> laneList){
        if (laneList.get(laneList.size()-1).terrain == Terrain.MUD){
            return false;
        } else if (laneList.get(laneList.size()-1).terrain == Terrain.WALL){
            return false;
        } else if (laneList.get(laneList.size()-1).terrain == Terrain.OIL_SPILL){
            return false;
        } else if (laneList.get(laneList.size()-1).isOccupiedByCyberTruck){
            return false;
        }
        
        return true;
    }

    // pengecekan berapa speed selanjutnya
    private int getNextSpeed(int currentSpeed){

        if (currentSpeed == 3){
            return 5;
        } else if (currentSpeed == 5){
            return 6;
        } else if (currentSpeed == 6){
            return 8;
        } else if (currentSpeed == 8){
            return 9;
        } else if (currentSpeed == 9){
            return 9;
        } else if (currentSpeed == 15){
            return 15;
        } else { // speed 0
            return 3;
        }

    }

    //berpindah lane
    // private Command changeLane(int myCarPosition){
        
    // }
}
