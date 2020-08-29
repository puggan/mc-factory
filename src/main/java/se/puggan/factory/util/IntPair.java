package se.puggan.factory.util;

import java.util.Collection;

/**
 * A rpw for list of ints (a) sorted by another list of ints (b)
 * for exemple useful as `TreSet<IntPair>`
 */
public class IntPair implements Comparable<IntPair> {
    public final int a;
    public final int b;

    public IntPair(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int compareTo(IntPair other) {
        return b == other.b ? a - other.a : b - other.b;
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
