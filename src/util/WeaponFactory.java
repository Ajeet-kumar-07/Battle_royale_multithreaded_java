package util;
import model.Weapon;

import java.util.Random;

public class WeaponFactory {
    public static final Random random = new Random();
    public static Weapon getRandomWeapon(){
        int choice = random.nextInt(3);
        switch (choice){
            case 0:
                return new Weapon("knife",20,10);
            case 1:
                return new Weapon("Pistol",30 , 15);
            case 2:
                return new Weapon("Rifle",45,25);
        }
        throw new IllegalStateException("Unexpected weapon choice: " + choice);
    }
}
