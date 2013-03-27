package local.techsy730.testrun;

public class GenericParamReflectionTestRuns
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        @SuppressWarnings("unchecked")
        final Class<? extends GenericTop<?, ?, ?>> theClassToTest = (Class<? extends GenericTop<?, ?, ?>>)
            AllGone.class;
        System.out.println(java.util.Arrays.toString(
            theClassToTest.getTypeParameters()));
        java.lang.reflect.Type[] interfaces = theClassToTest.getGenericInterfaces();
        System.out.println(java.util.Arrays.toString(interfaces));
        System.out.println(interfaces[0].getClass());
        Class<?> superInterface = (Class<?>)((java.lang.reflect.ParameterizedType)interfaces[0]).getRawType();
        System.out.println(
    }
    
    private static interface GenericTop<T1, T2, T3>{}
    
    private static interface OneLevelDown<T1, T2, T3> extends GenericTop<T1, T2, T3>{}
    
    private static interface OneParamGone<T2, T3> extends OneLevelDown<Integer, T2, T3>{}
    
    private static interface TwoParamGone<T3> extends OneParamGone<String, T3>{}
    
    private static interface PassThrough<T> extends TwoParamGone<T>{}
    
    private static interface AllGone extends PassThrough<Long>{}
    

}
