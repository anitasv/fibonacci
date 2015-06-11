import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public class Main {

    class Concept<T> implements Supplier<T> {

        private final Supplier<T> actual;

        Concept(Function<Concept<T>, Supplier<T>> actualFunc) {
            this.actual = actualFunc.apply(this);
        }

        @Override
        public T get() {
            return actual.get();
        }
    }

    class CachedSupplier<T> implements Supplier<T> {

        private final Supplier<T> actual;

        CachedSupplier(Supplier<T> actual) {
            this.actual = actual;
        }

        private volatile T value;
        private final AtomicBoolean computed = new AtomicBoolean(false);

        @Override
        public T get() {
            if (computed.compareAndSet(false, true)) {
                value = actual.get();
            }
            return value;
        }
    }

    class LazyList<T> {

        public final Supplier<T> car;

        public final Supplier<LazyList<T>> cdr;

        LazyList(Supplier<T> car, Supplier<LazyList<T>> cdr) {
            this.car = car;
            this.cdr = cdr;
        }
    }

    <T> Supplier<T> mem(Supplier<T> actual) {
        if (actual instanceof CachedSupplier) {
            return actual;
        }
        return new CachedSupplier<T>(actual);
    }

    <T> Supplier<LazyList<T>> cons2(Supplier<T> car, Supplier<LazyList<T>> cdr) {
        return mem(() -> new LazyList<>(car, cdr));
    }

    <T> Supplier<LazyList<T>> cons(T car, Supplier<LazyList<T>> cdr) {
        return cons2(() -> car, cdr);
    }

    <T> Supplier<LazyList<T>> tail(Supplier<LazyList<T>> list) {
        return mem(() -> list.get().cdr.get());
    }

    <T> Supplier<T> first(Supplier<LazyList<T>> list) {
        return mem(() -> list.get().car.get());
    }

    <T> Supplier<T> apply(BinaryOperator<T> operator, Supplier<T> op1, Supplier<T> op2) {
        return mem(() -> operator.apply(op1.get(), op2.get()));
    }

    <T> Supplier<T> lazy(Supplier<Supplier<T>> action) {
        return mem(() -> action.get().get());
    }

    <T> Supplier<T> concept(Function<Concept<T>, Supplier<T>> lazy) {
        return new Concept<>(lazy);
    }

    <T> Supplier<LazyList<T>> zip(BinaryOperator<T> reducer,
                                  Supplier<LazyList<T>> op1,
                                  Supplier<LazyList<T>> op2) {
        return cons2(apply(reducer, first(op1), first(op2)),
                lazy(() -> zip(reducer, tail(op1), tail(op2))));
    }

    BinaryOperator<Integer> sum = (x, y) -> {
        System.out.printf("Computing: %d + %d\n", x, y);
        return x + y;
    };


    private <T> void printList(Supplier<LazyList<T>> list, int count) {
        T val;
        while (count-- > 0 && ((val = first(list).get()) != null)) {
            System.out.println(val);
            list = tail(list);
        }
    }

    void run() {
        Supplier<LazyList<Integer>> lazyFib =
                concept(fib -> cons(0, cons(1, zip(sum, fib, tail(fib)))));
        printList(lazyFib, 10);
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
