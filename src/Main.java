import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class Main {

    class DelayedSupplier<T> implements Supplier<T> {

        Supplier<T> actual;

        @Override
        public T get() {
            return actual.get();
        }
    }

    class CachedSupplier<T> implements Supplier<T> {

        final Supplier<T> actual;

        CachedSupplier(Supplier<T> actual) {
            this.actual = actual;
        }

        volatile T value;
        final AtomicBoolean computed = new AtomicBoolean(false);

        @Override
        public T get() {
            if (computed.compareAndSet(false, true)) {
                value = actual.get();
            }
            return value;
        }
    }

    class LazyList<T> {

        final Supplier<T> car;

        final Supplier<LazyList<T>> cdr;

        LazyList(Supplier<T> car, Supplier<LazyList<T>> cdr) {
            this.car = car;
            this.cdr = cdr;
        }
    }

    <T> Supplier<T> mem(Supplier<T> actual) {
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

    <T> Supplier<LazyList<T>> zip(BinaryOperator<T> reducer,
                                  Supplier<LazyList<T>> op1,
                                  Supplier<LazyList<T>> op2) {
        return cons2(apply(reducer, first(op1), first(op2)), mem(() -> zip(reducer, tail(op1), tail(op2)).get()));
    }

    BinaryOperator<Integer> sum = (x, y) -> {
        System.out.printf("Computing: %d + %d\n", x, y);
        return x + y;
    };

    void run() {
        DelayedSupplier<LazyList<Integer>> fib = new DelayedSupplier<>();
        fib.actual = cons(0, cons(1, zip(sum, fib, tail(fib))));
        printList(fib.get(), 10);
    }

    private void printList(LazyList<Integer> list, int count) {
        while (count-- > 0 && list.car.get() != null) {
            System.out.println(list.car.get());
            list = list.cdr.get();
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
