package local.techsy730.function;

public interface Function3<R, T1, T2, T3>
    extends
        TwoOrMoreParameterFunction<R, T1, T2, ParameterPair<T2, T3>, ParameterPair<T1, T3>, T3,
            ParameterPair<T1, ParameterPair<T2, T3>>,
            Function1<R, T3>,
            Function2<R, T2, T3>,
            Function2<R, T1, T3>>,
        FunctionBase<R, ParameterPair<T1, ParameterPair<T2, T3>>>
{
    public R call(T1 arg1, T2 arg2, T3 arg3);
    
    public Function2<R, T1, T2> bind3(T3 argument);
}
