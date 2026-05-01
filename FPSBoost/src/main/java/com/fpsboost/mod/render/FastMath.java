package com.fpsboost.mod.render;

/**
 * Replaces expensive Math.sin / Math.cos calls with lookup tables.
 * Table resolution: 0.1-degree increments (3600 entries).
 *
 * Usage:
 *   FastMath.sin(radians)  → float
 *   FastMath.cos(radians)  → float
 *   FastMath.sqrt(value)   → float  (fast approximation, ~0.1% error)
 */
public final class FastMath {

    private static final int    TABLE_SIZE  = 65536;         // power-of-2 for fast modulo
    private static final float  TABLE_SCALE = TABLE_SIZE / (2f * (float) Math.PI);
    private static final float  TABLE_SCALE_INV = 1f / TABLE_SCALE;

    private static final float[] SIN_TABLE = new float[TABLE_SIZE + 1];

    private FastMath() {}

    /**
     * Must be called once during mod preInit before any FastMath calls.
     */
    public static void init() {
        for (int i = 0; i <= TABLE_SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * TABLE_SCALE_INV);
        }
    }

    /** Fast sine using lookup table. Input in radians. */
    public static float sin(float radians) {
        // Map radians to table index using bitwise AND (TABLE_SIZE must be power of 2)
        return SIN_TABLE[Math.round(radians * TABLE_SCALE) & (TABLE_SIZE - 1)];
    }

    /** Fast cosine: cos(x) = sin(x + π/2) */
    public static float cos(float radians) {
        return SIN_TABLE[(Math.round(radians * TABLE_SCALE) + (TABLE_SIZE / 4)) & (TABLE_SIZE - 1)];
    }

    /**
     * Fast inverse square root (Quake III algorithm adapted).
     * Error: < 0.2%
     */
    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int   i     = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);           // magic constant
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x);     // Newton-Raphson iteration
        return x;
    }

    /** Fast square root via invSqrt. */
    public static float sqrt(float x) {
        if (x <= 0f) return 0f;
        return x * invSqrt(x);
    }

    /** Fast floor without boxing overhead. */
    public static int floor(float x) {
        int xi = (int) x;
        return (x < xi) ? xi - 1 : xi;
    }

    /** Fast abs for float. */
    public static float abs(float x) {
        return x < 0f ? -x : x;
    }

    /**
     * Linearly interpolate between a and b by t (0..1).
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Clamp value to [min, max].
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
