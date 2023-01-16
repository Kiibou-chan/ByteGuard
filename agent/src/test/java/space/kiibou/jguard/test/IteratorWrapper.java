package space.kiibou.jguard.test;

import java.util.Iterator;

public class IteratorWrapper<T> implements Iterator<T> {

    private final Iterator<T> wrapped;

    public IteratorWrapper(final Iterator<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public T next() {
        return wrapped.next();
    }

}
