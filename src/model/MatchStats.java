package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchStats {
    private int totalAttacks;
    private int totalDamageDealt;
    private int successfulAttacks;
    private int missedAttacks;
    private int totalZoneDamage;
    private String longestSurvivor;
    private int longestSurvivalRounds;

    private String firstBloodKiller;
    private String highestKillStreakPlayer;
    private int highestKillStreak;
    private String zoneSurvivor;
    private int zoneSurvivorTicks;
    private String clutchWinner;
    private int clutchWinnerHealth;

    private final Map<String, Integer> currentKillStreak;
    private final Map<String, Integer> zoneDamageTicks;

    public MatchStats() {
        this.totalAttacks = 0;
        this.totalDamageDealt = 0;
        this.successfulAttacks = 0;
        this.missedAttacks = 0;
        this.totalZoneDamage = 0;
        this.longestSurvivor = "";
        this.longestSurvivalRounds = 0;
        this.firstBloodKiller = "";
        this.highestKillStreakPlayer = "";
        this.highestKillStreak = 0;
        this.zoneSurvivor = "";
        this.zoneSurvivorTicks = 0;
        this.clutchWinner = "";
        this.clutchWinnerHealth = -1;
        this.currentKillStreak = new HashMap<>();
        this.zoneDamageTicks = new HashMap<>();
    }

    public void recordAttack(boolean hit, int damage) {
        totalAttacks++;
        if (hit) {
            successfulAttacks++;
            totalDamageDealt += damage;
        } else {
            missedAttacks++;
        }
    }

    public void updateLongestSurvivor(String playerId, int rounds) {
        if (rounds > longestSurvivalRounds) {
            longestSurvivor = playerId;
            longestSurvivalRounds = rounds;
        }
    }

    public synchronized List<String> recordElimination(String killerId, String victimId) {
        List<String> unlocked = new ArrayList<>();

        if (firstBloodKiller.isEmpty()) {
            firstBloodKiller = killerId;
            unlocked.add("First Blood: " + killerId);
        }

        int streak = currentKillStreak.getOrDefault(killerId, 0) + 1;
        currentKillStreak.put(killerId, streak);
        currentKillStreak.put(victimId, 0);

        if (streak > highestKillStreak) {
            highestKillStreak = streak;
            highestKillStreakPlayer = killerId;
        }

        if (streak == 3) {
            unlocked.add("Kill Streak x3: " + killerId);
        } else if (streak == 5) {
            unlocked.add("Rampage x5: " + killerId);
        }

        return unlocked;
    }

    public synchronized void recordDeath(String playerId) {
        currentKillStreak.put(playerId, 0);
    }

    public synchronized void recordZoneDamage(String playerId, int damage) {
        totalZoneDamage += damage;
        zoneDamageTicks.put(playerId, zoneDamageTicks.getOrDefault(playerId, 0) + 1);
    }

    public synchronized String finalizeZoneSurvivorAchievement() {
        int bestTicks = 0;
        String bestPlayer = "";

        for (Map.Entry<String, Integer> entry : zoneDamageTicks.entrySet()) {
            if (entry.getValue() > bestTicks) {
                bestTicks = entry.getValue();
                bestPlayer = entry.getKey();
            }
        }

        if (bestTicks <= 0 || bestPlayer.isEmpty()) {
            return null;
        }

        zoneSurvivor = bestPlayer;
        zoneSurvivorTicks = bestTicks;
        return "Zone Survivor: " + zoneSurvivor + " endured " + zoneSurvivorTicks + " zone ticks";
    }

    public synchronized String recordClutchWin(String winnerId, int winnerHealth) {
        if (winnerHealth > 20) {
            return null;
        }

        clutchWinner = winnerId;
        clutchWinnerHealth = winnerHealth;
        return "Clutch Win: " + clutchWinner + " survived with " + clutchWinnerHealth + " HP";
    }

    public String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== MATCH STATISTICS ===\n");
        report.append("Total Attacks: ").append(totalAttacks).append("\n");
        report.append("Successful Hits: ").append(successfulAttacks).append("\n");
        report.append("Missed Attacks: ").append(missedAttacks).append("\n");
        report.append("Total Damage Dealt: ").append(totalDamageDealt).append("\n");
        report.append("Total Zone Damage: ").append(totalZoneDamage).append("\n");
        if (!longestSurvivor.isEmpty()) {
            report.append("Longest Survivor: ").append(longestSurvivor)
                  .append(" (").append(longestSurvivalRounds).append(" rounds)\n");
        }
        report.append("\n=== ACHIEVEMENTS ===\n");
        if (!firstBloodKiller.isEmpty()) {
            report.append("First Blood: ").append(firstBloodKiller).append("\n");
        }
        if (!highestKillStreakPlayer.isEmpty()) {
            report.append("Highest Kill Streak: ").append(highestKillStreakPlayer)
                  .append(" x").append(highestKillStreak).append("\n");
        }
        if (!zoneSurvivor.isEmpty()) {
            report.append("Zone Survivor: ").append(zoneSurvivor)
                  .append(" (").append(zoneSurvivorTicks).append(" ticks)\n");
        }
        if (!clutchWinner.isEmpty()) {
            report.append("Clutch Winner: ").append(clutchWinner)
                  .append(" (").append(clutchWinnerHealth).append(" HP)\n");
        }
        return report.toString();
    }

    // Getters
    public int getTotalAttacks() { return totalAttacks; }
    public int getTotalDamageDealt() { return totalDamageDealt; }
    public int getSuccessfulAttacks() { return successfulAttacks; }
    public int getMissedAttacks() { return missedAttacks; }
    public int getTotalZoneDamage() { return totalZoneDamage; }
    public String getLongestSurvivor() { return longestSurvivor; }
    public int getLongestSurvivalRounds() { return longestSurvivalRounds; }
    public String getFirstBloodKiller() { return firstBloodKiller; }
    public String getHighestKillStreakPlayer() { return highestKillStreakPlayer; }
    public int getHighestKillStreak() { return highestKillStreak; }
    public String getZoneSurvivor() { return zoneSurvivor; }
    public int getZoneSurvivorTicks() { return zoneSurvivorTicks; }
    public String getClutchWinner() { return clutchWinner; }
    public int getClutchWinnerHealth() { return clutchWinnerHealth; }
}
