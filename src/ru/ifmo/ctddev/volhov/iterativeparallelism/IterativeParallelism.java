package ru.ifmo.ctddev.volhov.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class specifies the functions over list that can be executed simultaneously in the given
 * number of distinct threads. It does not use any high-abstract tools implemented in {@link java.util.concurrent}
 * class.
 * <p>
 * The inner nature of class is based on {@link ru.ifmo.ctddev.volhov.iterativeparallelism.ConcUtils} class,
 * that gives an access to parallel functions, similar to {@code foldl} and {@code map}. As many of operations
 * are associative ({@link #minimum}, {@link #all}), it also uses the
 * {@link ru.ifmo.ctddev.volhov.iterativeparallelism.Monoid} class to represent this abstraction.
 * <p>
 * It also uses {@link ParallelMapperImpl} to execute tasks on number of threads specified in that object.
 *
 * @author volhovm
 * @see ru.ifmo.ctddev.volhov.iterativeparallelism.ConcUtils
 * @see ru.ifmo.ctddev.volhov.iterativeparallelism.Monoid
 * @see ru.ifmo.ctddev.volhov.iterativeparallelism.ParallelMapperImpl
 */
public class IterativeParallelism implements ListIP {
    ParallelMapper parallelMapper;
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }
    public IterativeParallelism() {}

    /**
     * Concatenates the string values of elements in the given lists.
     * This method takes number of threads and list of objects, than gets {@link Object#toString()} of every
     * objects and concatenates them in the order, specified in the list, using given numbers of threads,
     * simultaneously.
     *
     * @param threads number of threads
     * @param values  list of objects string value of which method will concatenate
     *
     * @return string, made of concatenation of objects' string representations
     */
    @Override
    public String concat(int threads, List<?> values) throws InterruptedException {
        return String.join("", ConcUtils.map(Object::toString, values, threads));
    }

    /**
     * Returns the list, containing of objects in the initial list, that satisfy the given predicate.
     * This method does it on threads number given, dividing the list into {@code threads} different sublists,
     *
     * @param threads   number of threads
     * @param values    list of values to filter
     * @param predicate predicate
     * @param <T>       type of elements in the list
     *
     * @return list, filtered with the predicate
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
//        return ConcUtils.foldl(
//                Monoid.<T>listConcatWithPred((a, b) -> !b.isEmpty() && predicate.test(b.get(0))),
//                values.stream().map(a -> new ArrayList<T>(Arrays.asList(a))).collect(Collectors.toList()),
//                threads);
        return ConcUtils.foldl(Monoid.<T>listConcat(),
                (List<T> lst) -> lst.stream().filter(predicate).collect(Collectors.toList()),
                parallelMapper == null ? Optional.empty() : Optional.<ParallelMapper>of(parallelMapper),
                values,
                threads);
//        )
    }

    /**
     * Returns the list, mapped with the given function -- it returns the list, in which every element is
     * the function result of application to the corresponding item in the given list. It does this
     * simultaneously using the number of threads given.
     *
     * @param threads number of threads
     * @param values  the list to map
     * @param f       function, that transforms element from initial list to the element in result
     * @param <T>     type of function domain and the elements of given list
     * @param <U>     type of function range and the elements of return list
     *
     * @return mapped list
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f)
            throws InterruptedException {
        if (parallelMapper == null) {
            return ConcUtils.map(f, values, threads);
        } else {
            return ConcUtils.<T, List<U>>foldl(Monoid.listConcat(),
                    (List<T> lst) -> lst.stream().sequential().map(f).collect(Collectors.toList()),
                    Optional.of(parallelMapper),
                    values,
                    threads);
        }
    }

    /**
     * Returns the first minimum in the list, specified with given comparator. It does it simultaneously
     * on the number of threads given.
     *
     * @param threads    number of threads
     * @param values     initial list to find minimum in
     * @param comparator comparator on elements of the list
     * @param <T>        type of elements in the list
     *
     * @return first minimum
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        BinaryOperator<T> max = (a, b) -> comparator.compare(a, b) < 0 ? b : a;
        if (parallelMapper == null) {
            return ConcUtils.foldl1(max, values, threads);
        } else {
            return ConcUtils.<T, T>foldl(new Monoid<T>(max),
                    (List<T> lst) -> lst.stream().reduce(max).get(),
                    Optional.of(parallelMapper),
                    values,
                    threads);
        }
    }

    /**
     * Returns the first maximum in the list, specified with given comparator. It does it simultaneously
     * on the number of threads given.
     *
     * @param threads    number of threads
     * @param values     initial list to find maximum in
     * @param comparator comparator on elements of the list
     * @param <T>        type of elements in the list
     *
     * @return first maximum
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns if all elements of list satisfy the given predicate. It does it simultaneously
     * on the number of threads given.
     *
     * @param threads   number of threads
     * @param values    initial list
     * @param predicate predicate to test items of list
     * @param <T>       type of elements in the list
     *
     * @return true, if all elements of the list satisfy predicate. False otherwise
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        if (parallelMapper == null) {
            return ConcUtils.<T, Boolean>concatmap(Monoid.boolAnd(true),
                    a -> a.stream().<Boolean>map(predicate::test).reduce((x, y) -> x && y).get(),
                    values,
                    threads);
        } else {
            return ConcUtils.<T, Boolean>foldl(Monoid.boolAnd(true),
                    (List<T> lst) -> lst.stream().map(predicate::test).reduce(Boolean::logicalAnd).get(),
                    Optional.of(parallelMapper),
                    values,
                    threads);
        }
    }

    /**
     * Returns if any element of list satisfy the given predicate. It does it simultaneously
     * on the number of threads given.
     *
     * @param threads   number of threads
     * @param values    initial list
     * @param predicate predicate to test items of list
     * @param <T>       type of elements in the list
     *
     * @return true, if there is element in the list satisfying predicate. False otherwise
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}
