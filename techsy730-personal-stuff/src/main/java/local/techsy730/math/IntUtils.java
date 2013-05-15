package local.techsy730.math;

public final class IntUtils
{
    public static final int clamp(long val, int min, int max)
    {
        return val <= min ? min : (val >= max ? max : (int)val); 
    }
    
}
