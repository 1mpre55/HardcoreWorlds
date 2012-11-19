package com.impress.hardcoreworlds;

import java.io.Serializable;
import java.util.Calendar;

public class PlayerLives  implements Serializable{
	private static final long serialVersionUID = 5839218588933443512L;
	int lives;
	Calendar regenUpdate;
	boolean regenerating;
	public PlayerLives(int lives) {
		this.lives = lives;
	}
	boolean hasLives() {
		update();
		return lives > 0;
	}
	void loseLife() {
		resetRegen();
		lives--;
	}
	void update() {
		regenerating = (Bans.regen && lives < Bans.maxLives && (regenerating || Bans.regenTrigger < 0 || lives <= Bans.regenTrigger));
		if (regenerating) regen();
	}
	void regen() {
		Calendar now = Calendar.getInstance();
		for (; lives < Bans.maxLives; lives += Bans.regenValue) {
			regenUpdate.add(Calendar.MINUTE, Bans.regenTime);
			if (regenUpdate.after(now)) {
				regenUpdate.add(Calendar.MINUTE, 0 - Bans.regenTime);
				break;
			}
		}
		if (lives >= Bans.maxLives) regenerating = false;
	}
	void resetRegen() {
		update();
		regenUpdate = Calendar.getInstance();
	}
}