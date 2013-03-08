package local.techsy730.function;

public interface OneOrMoreParameterFunction<R, FirstType, AggregateWithNoFirstType, AggregateType,
    Bound1stFunctionType extends FunctionBase<R, AggregateWithNoFirstType>>
    extends FunctionBase<R, AggregateType>
{
    public Bound1stFunctionType bind1(FirstType argument);
}
