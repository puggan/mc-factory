package se.puggan.factory.util;

import java.util.Collection;

/**
 * A rpw for list of ints (a) sorted by another list of ints (b)
 * for exemple useful as `TreSet<IntPair>`
 */
public class IntPair implements Comparable<IntPair> {
    private final int a;
    private final int b;

    public IntPair(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int compareTo(IntPair other) {
        return b - other.b;
    }

    public static int[] aArray(Collection<IntPair> list) {
        int[] array = new int[list.size()];
        int index = 0;
        for (IntPair ip : list) {
            array[index++] = ip.a;
        }
        return array;
    }
}
