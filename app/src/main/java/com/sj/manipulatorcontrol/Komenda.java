package com.sj.manipulatorcontrol;

import java.util.Objects;

public class Komenda {
    private final int speed;
    private final int direction;

    public Komenda() {
        this.speed = 0;
        this.direction = 0;
    }

    public Komenda(int speed, int direction) {
        this.speed = speed;
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Komenda komenda = (Komenda) o;
        return speed == komenda.speed && direction == komenda.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(speed, direction);
    }

    @Override
    public String toString() {
        return "{\"spd\":" + speed + ",\"dir\":" + direction + "}!";
    }
}
