package local.techsy730.util;

public final class CollectionUtils
{
    private CollectionUtils(){} //Utility class
    
    private static final class MapAsList<E> extends java.util.AbstractList<E> implements java.util.RandomAccess
    {
        
        private final java.util.Map<? super Integer, E> wrapped;
        private volatile int size;
        
        MapAsList(java.util.Map<? super Integer, E> toWrap)
        {
            assert toWrap.isEmpty();
            this.wrapped = toWrap;
            size = 0;
        }

        @Override
        public E get(int index)
        {
            if(index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
            return wrapped.get(index);
        }
        
        @Override
        public synchronized boolean add(E e)
        {
            add0(e);
            return true;
        }
        
        private void add0(E e)
        {
            wrapped.put(size++, e);
        }
        
        @Override
        public synchronized void add(int index, E element)
        {
            if(index < 0 || index > size)
                throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
            if(index != size)
                throw new IllegalArgumentException("Can only insert at the end of a Map backed List. " + 
                    "index: " + index + " size: " + size);
            add(element);
        }
        
        @Override
        public synchronized boolean addAll(java.util.Collection<? extends E> c)
        {
            for(E e : c)
                add0(e);
            return true;
        }
        
        @Override
        public synchronized boolean addAll(int index, java.util.Collection<? extends E> c)
        {
            if(index < 0 || index > size)
                throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
            if(index != size)
                throw new IllegalArgumentException("Can only insert at the end of a Map backed List. " + 
                    "index: " + index + " size: " + size);
            for(E e : c)
                add0(e);
            return true;
        }
        
        @Override
        public synchronized E remove(int index)
        {
            if(index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
            if(index != size - 1)
                throw new IllegalArgumentException("Can only remove at the end of a Map backed List. " + 
                    "index: " + index + " size: " + size);
            return wrapped.remove(--size);
        }
        
        @Override
        public java.util.Iterator<E> iterator()
        {
            //If we were given a SortedMap, then the value set is already in the correct index order
            if(wrapped instanceof java.util.SortedMap)
                com.google.common.collect.Iterators.unmodifiableIterator(wrapped.values().iterator());
            //Otherwise, let AbstractList take care of it
            return com.google.common.collect.Iterators.unmodifiableIterator(super.iterator());
        }
        
        @Override
        public E set(int index, E element)
        {
            if(index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
            return wrapped.put(index, element);
        }

        @Override
        public int size()
        {
            return size;
        }
        
        @Override
        public synchronized void clear()
        {
            wrapped.clear();
            size = 0;
        }
        
    }
}
