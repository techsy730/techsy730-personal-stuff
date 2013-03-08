package local.techsy730.function;

//Due to the explosion of complexity with the needed generic arguments to be type safe
//over the number of arguments of the parameter,
//it is not worth it to do the ThreeOrMoreParameterFunction or beyond.
//All bindn for N >= 3 && 2<n<=N will be in the respective FunctionN interfaces
public interface TwoOrMoreParameterFunction<R, FirstType, SecondType, AggregateWithNoFirstType, AggregateWithNoSecondType, AggregateWithNoFirstOrSecondType,
        AggregateType extends ParameterPair<FirstType, AggregateWithNoFirstType>,
        Bound1stAnd2ndType extends FunctionBase<R, AggregateWithNoFirstOrSecondType>,
        Bound1stFunctionType extends OneOrMoreParameterFunction<R, SecondType, AggregateWithNoFirstOrSecondType, AggregateWithNoFirstType, Bound1stAnd2ndType>,
        Bound2ndFunctionType extends OneOrMoreParameterFunction<R, FirstType, AggregateWithNoFirstOrSecondType, AggregateWithNoSecondType, Bound1stAnd2ndType>>
    extends 
        OneOrMoreParameterFunction<R, FirstType, AggregateWithNoFirstType, AggregateType, Bound1stFunctionType>,
        FunctionBase<R, AggregateType>
{
    public Bound2ndFunctionType bind2(SecondType argument);
}
