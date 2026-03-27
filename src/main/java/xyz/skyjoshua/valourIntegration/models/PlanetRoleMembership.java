package xyz.skyjoshua.valourIntegration.models;

public class PlanetRoleMembership {
    public long rf0;
    public long rf1;
    public long rf2;
    public long rf3;

    public long[] toArray() {
        return new long[]{ rf0, rf1, rf2, rf3 };
    }

    public boolean hasRole(int index) {
        if (index < 0 || index >= 256) return false;
        int block = index >> 6;
        int offset = index & 63;
        return (toArray()[block] & (1L << offset)) != 0;
    }

    public int getRoleCount() {
        return Long.bitCount(rf0) + Long.bitCount(rf1) + Long.bitCount(rf2) + Long.bitCount(rf3);
    }

    public int[] getRoleIndices() {
        int[] result = new int[getRoleCount()];
        int index = 0;
        long[] fields = toArray();

        for (int block = 0; block < 4; block++) {
            long bits = fields[block];
            while (bits != 0) {
                int tzc = Long.numberOfTrailingZeros(bits);
                result[index++] = (block << 6) + tzc;
                bits &= ~(1L << tzc);
            }
        }

        return result;
    }

    public boolean equals(PlanetRoleMembership other) {
        return rf0 == other.rf0 && rf1 == other.rf1 && rf2 == other.rf2 && rf3 == other.rf3;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(rf0, rf1, rf2, rf3);
    }
}