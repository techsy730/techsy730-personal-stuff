package local.techsy730.util.prim.coll.ints;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;

import local.techsy730.ref.ReferenceType;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import static local.techsy730.ref.References.unwrapSafe;

public class Int2WeakReferenceMap<V> extends AbstractInt2ObjectMap<V> implements Int2ObjectMap<V>
{

    private final Int2ObjectMap<Reference<V>> wrapped;
    private final ReferenceType refType;
    private final ReferenceQueue<V> queue;
    
    public Int2WeakReferenceMap(Int2ObjectMap<Reference<V>> toWrap, ReferenceType refType)
    {
        this.wrapped = toWrap;
        this.refType = refType;
        queue = new ReferenceQueue<>();
    }
    
    public Int2WeakReferenceMap(Int2ObjectMap<Reference<V>> toWrap)
    {
        this(toWrap, ReferenceType.WEAK);
    }
    
    public Reference<V> put(Integer key, Reference<V> value)
    {
        return wrapped.put(key, value);
    }

    @Override
    public V get(Object key)
    {
        return unwrapSafe(wrapped.get(key));
    }

    @Override
    public boolean containsKey(Object key)
    {
        return wrapped.containsKey(key);
    }

    @Override
    public V put(int key, V value)
    {
        if(value == null)
            throw new NullPointerException("null values not supported");
        return unwrapSafe(wrapped.put(key, refType.wrap(value, queue)));
    }

    @Override
    public V remove(Object key)
    {
        return unwrapSafe(wrapped.remove(key));
    }

    @Override
    public V get(int key)
    {
        return unwrapSafe(wrapped.get(key));
    }

    @Override
    public int size()
    {
        return wrapped.size();
    }

    @Override
    public V remove(int key)
    {
        return unwrapSafe(wrapped.remove(key));
    }

    @Override
    public boolean containsKey(int key)
    {
        return wrapped.containsKey(key);
    }

    @Override
    public void clear()
    {
        wrapped.clear();
    }

    @Override
    public IntSet keySet()
    {
        return wrapped.keySet();
    }

    @Override
    public boolean isEmpty()
    {
        return wrapped.isEmpty();
    }

    @Override
    public ObjectSet<it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<V>> int2ObjectEntrySet()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    

}
