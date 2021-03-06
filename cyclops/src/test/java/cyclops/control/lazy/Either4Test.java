package cyclops.control.lazy;

import cyclops.companion.Monoids;
import cyclops.companion.Reducers;
import cyclops.companion.Semigroups;
import cyclops.companion.Streams;
import cyclops.control.Future;
import com.oath.cyclops.util.box.Mutable;

import cyclops.control.*;
import cyclops.control.LazyEither4;
import cyclops.control.Maybe;
import cyclops.control.Trampoline;
import cyclops.data.Seq;
import cyclops.function.Monoid;

import cyclops.reactive.ReactiveSeq;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class Either4Test {

    int called = 0;
    @Before
    public void setup(){
        called =0;
    }
    @Test
    public void later(){


        LazyEither4<Object, Object, Object, Integer> e = LazyEither4.later(() -> {
            called++;
            return LazyEither4.right(10);
        });
        assertThat(called,equalTo(0));

        e.isRight();
        assertThat(called,equalTo(1));
        e.isRight();
        assertThat(called,equalTo(1));
    }
    @Test
    public void always(){


        LazyEither4<Object, Object, Object, Integer>  e = LazyEither4.always(() -> {
            called++;
            return LazyEither4.right(10);
        });
        assertThat(called,equalTo(0));

        e.isRight();
        assertThat(called,equalTo(1));
        e.isRight();
        assertThat(called,equalTo(2));
    }
    @Test
    public void nullPublisher() {
        assertThat(LazyEither4.fromPublisher(Seq.of(null, 190)), not(equalTo(LazyEither4.right(null))));
    }
    @Test
    public void fromNull(){
        System.out.println(LazyEither4.fromIterable(Seq.of().plus(null)));
        assertThat(LazyEither4.right(null), equalTo(LazyEither4.right(null)));

        assertThat(LazyEither4.fromIterable(Seq.of()),equalTo(LazyEither4.left1(null)));


        assertThat(LazyEither4.fromPublisher(Seq.of(null,190)),not(equalTo(LazyEither4.right(null))));
        assertTrue(LazyEither4.fromPublisher(Seq.of(null,190)).isLeft1());
        assertThat(LazyEither4.fromFuture(Future.ofResult(null)),equalTo(LazyEither4.right(null)));

    }
    boolean lazy = true;
    @Test
    public void lazyTest() {
        LazyEither4.right(10)
             .flatMap(i -> { lazy=false; return  LazyEither4.right(15);})
             .map(i -> { lazy=false; return  LazyEither4.right(15);})
             .map(i -> Maybe.of(20));


        assertTrue(lazy);

    }

    @Test
    public void mapFlatMapTest(){
        assertThat(LazyEither4.right(10)
               .map(i->i*2)
               .flatMap(i-> LazyEither4.right(i*4))
               .orElse(-10),equalTo(80));
    }
    @Test
    public void odd() {
        System.out.println(even(LazyEither4.right(200000)).get());
    }

    public LazyEither4<String,String,String,String> odd(LazyEither4<String,String,String,Integer> n) {

        return n.flatMap(x -> even(LazyEither4.right(x - 1)));
    }

    public LazyEither4<String,String,String,String> even(LazyEither4<String,String,String,Integer> n) {
        return n.flatMap(x -> {
            return x <= 0 ? LazyEither4.right("done") : odd(LazyEither4.right(x - 1));
        });
    }
    LazyEither4<String,String,String,Integer> just;
    LazyEither4<String,String,String,Integer> left2;
    LazyEither4<String,String,String,Integer> left3;
    LazyEither4<String,String,String,Integer> none;
    @Before
    public void setUp() throws Exception {
        just = LazyEither4.right(10);
        none = LazyEither4.left1("none");
        left2 = LazyEither4.left2("left2");
        left3 = LazyEither4.left3("left3");

    }
    @Test
    public void isLeftRight(){
        assertTrue(just.isRight());
        assertTrue(none.isLeft1());
        assertTrue(left2.isLeft2());
        assertTrue(left3.isLeft3());

        assertFalse(just.isLeft1());
        assertFalse(just.isLeft2());
        assertFalse(just.isLeft3());
        assertFalse(none.isLeft2());
        assertFalse(none.isLeft3());
        assertFalse(none.isRight());
        assertFalse(left2.isLeft1());
        assertFalse(left2.isLeft3());
        assertFalse(left2.isRight());
        assertFalse(left3.isLeft1());
        assertFalse(left3.isLeft2());
        assertFalse(left3.isRight());
    }



    @Test
    public void testTraverseLeft1() {
        Seq<LazyEither4<Integer,String,String,String>> list = Seq.of(just,none, LazyEither4.<String,String,String,Integer>right(1)).map(LazyEither4::swap1);
      LazyEither4<Integer, String, String, ReactiveSeq<String>> xors = LazyEither4.traverse(list, s -> "hello:" + s);
        assertThat(xors.map(s->s.toList()),equalTo(LazyEither4.right(Arrays.asList("hello:none"))));
    }
    @Test
    public void testSequenceLeft1() {
        Seq<LazyEither4<Integer,String,String,String>> list = Seq.of(just,none, LazyEither4.<String,String,String,Integer>right(1)).map(LazyEither4::swap1);
      LazyEither4<Integer, String, String, ReactiveSeq<String>> xors = LazyEither4.sequence(list);
        assertThat(xors.map(s->s.toList()),equalTo(LazyEither4.right(Arrays.asList("none"))));
    }
    @Test
    public void testSequenceLeft2() {
        Seq<LazyEither4<String,Integer,String,String>> list = Seq.of(just,left2, LazyEither4.<String,String,String,Integer>right(1)).map(LazyEither4::swap2);
      LazyEither4<String, Integer, String, ReactiveSeq<String>> xors = LazyEither4.sequence(list);
        assertThat(xors.map(s->s.toList()),equalTo(LazyEither4.right(Arrays.asList("left2"))));
    }


    @Test
    public void testAccumulate() {
      LazyEither4<String, String, String, Integer> iors = LazyEither4.accumulate(Monoids.intSum, Arrays.asList(none, just, LazyEither4.right(10)));
        assertThat(iors,equalTo(LazyEither4.right(20)));
    }

    @Test
    public void nest(){
       assertThat(just.nest().map(m->m.toOptional().get()),equalTo(just));
       assertThat(none.nest().map(m->m.get()),equalTo(none));
    }
    @Test
    public void coFlatMap(){
        assertThat(just.coflatMap(m-> m.isPresent()? m.toOptional().get() : 50),equalTo(just));
        assertThat(none.coflatMap(m-> m.isPresent()? m.toOptional().get() : 50),equalTo(LazyEither4.right(50)));
    }

    @Test
    public void visit(){

        assertThat(just.fold(secondary->"no", left2->"left2", left3->"left3", primary->"yes"),equalTo("yes"));
        assertThat(none.fold(secondary->"no", left2->"left2", left3->"left3", primary->"yes"),equalTo("no"));
        assertThat(left2.fold(secondary->"no", left2->"left2", left3->"left3", primary->"yes"),equalTo("left2"));
        assertThat(left3.fold(secondary->"no", left2->"left2", left3->"left3", primary->"yes"),equalTo("left3"));
    }

    @Test
    public void testToMaybe() {
        assertThat(just.toMaybe(),equalTo(Maybe.of(10)));
        assertThat(none.toMaybe(),equalTo(Maybe.nothing()));
    }

    private int add1(int i){
        return i+1;
    }

    @Test
    public void testOfT() {
        assertThat(Ior.right(1),equalTo(Ior.right(1)));
    }









    @Test
    public void testUnitT() {
        assertThat(just.unit(20),equalTo(LazyEither4.right(20)));
    }



    @Test
    public void testisPrimary() {
        assertTrue(just.isRight());
        assertFalse(none.isRight());
    }


    @Test
    public void testMapFunctionOfQsuperTQextendsR() {
        assertThat(just.map(i->i+5),equalTo(LazyEither4.right(15)));
        assertThat(none.map(i->i+5),equalTo(LazyEither4.left1("none")));
    }

    @Test
    public void testFlatMap() {
        assertThat(just.flatMap(i-> LazyEither4.right(i+5)),equalTo(LazyEither4.right(15)));
        assertThat(none.flatMap(i-> LazyEither4.right(i+5)),equalTo(LazyEither4.left1("none")));
    }

    @Test
    public void testWhenFunctionOfQsuperTQextendsRSupplierOfQextendsR() {
        assertThat(just.fold(i->i+1,()->20),equalTo(11));
        assertThat(none.fold(i->i+1,()->20),equalTo(20));
    }


    @Test
    public void testStream() {
        assertThat(just.stream().toList(),equalTo(Arrays.asList(10)));
        assertThat(none.stream().toList(),equalTo(Arrays.asList()));
    }

    @Test
    public void testOfSupplierOfT() {

    }

    @Test
    public void testConvertTo() {

        Stream<Integer> toStream = just.fold(m->Stream.of(m),()->Stream.of());
        assertThat(toStream.collect(Collectors.toList()),equalTo(Arrays.asList(10)));
    }


    @Test
    public void testConvertToAsync() {
        Future<Stream<Integer>> async = Future.of(()->just.fold(f->Stream.of((int)f),()->Stream.of()));

        assertThat(async.orElse(Stream.empty()).collect(Collectors.toList()),equalTo(Arrays.asList(10)));
    }

    @Test
    public void testIterate() {
        assertThat(just.iterate(i->i+1,-1000).limit(10).sumInt(i->i),equalTo(145));
    }

    @Test
    public void testGenerate() {
        assertThat(just.generate(-10000).limit(10).sumInt(i->i),equalTo(100));
    }



    @Test
    public void testFoldMonoidOfT() {
        assertThat(just.fold(Reducers.toTotalInt()),equalTo(10));
    }




    @Test
    public void testToTry() {
        assertTrue(none.toTry().isFailure());
        assertThat(just.toTry(),equalTo(Try.success(10)));
    }

    @Test
    public void testToTryClassOfXArray() {
        assertTrue(none.toTry(Throwable.class).isFailure());
    }

    @Test
    public void testToIor() {
        assertThat(just.toIor(),equalTo(Ior.right(10)));

    }
    @Test
    public void testToIorNone(){
        Ior<String,Integer> ior = none.toIor();
        assertTrue(ior.isLeft());
        assertThat(ior,equalTo(Ior.left("none")));

    }


    @Test
    public void testToIorSecondary() {
        assertThat(just.toIor().swap(),equalTo(Ior.left(10)));
    }


    @Test
    public void testToIorSecondaryNone(){
        Ior<Integer,String> ior = none.toIor().swap();
        assertTrue(ior.isRight());
        assertThat(ior,equalTo(Ior.right("none")));

    }




    @Test
    public void testMkString() {
        assertThat(just.mkString(),equalTo("Either4.right[10]"));
        assertThat(none.mkString(),equalTo("Either4.left1[none]"));
    }

    @Test
    public void testGet() {
        assertThat(just.get(),equalTo(Option.some(10)));
    }


    @Test
    public void testFilter() {
        assertFalse(just.filter(i->i<5).isPresent());
        assertTrue(just.filter(i->i>5).isPresent());
        assertFalse(none.filter(i->i<5).isPresent());
        assertFalse(none.filter(i->i>5).isPresent());

    }

    @Test
    public void testOfType() {
        assertFalse(just.ofType(String.class).isPresent());
        assertTrue(just.ofType(Integer.class).isPresent());
        assertFalse(none.ofType(String.class).isPresent());
        assertFalse(none.ofType(Integer.class).isPresent());
    }

    @Test
    public void testFilterNot() {
        assertTrue(just.filterNot(i->i<5).isPresent());
        assertFalse(just.filterNot(i->i>5).isPresent());
        assertFalse(none.filterNot(i->i<5).isPresent());
        assertFalse(none.filterNot(i->i>5).isPresent());
    }

    @Test
    public void testNotNull() {
        assertTrue(just.notNull().isPresent());
        assertFalse(none.notNull().isPresent());

    }








    @Test
    public void testFoldRightMonoidOfT() {
        assertThat(just.fold(Monoid.of(1,Semigroups.intMult)),equalTo(10));
    }


    @Test
    public void testWhenFunctionOfQsuperMaybeOfTQextendsR() {
        assertThat(just.fold(s->"hello", ()->"world"),equalTo("hello"));
        assertThat(none.fold(s->"hello", ()->"world"),equalTo("world"));
    }


    @Test
    public void testOrElseGet() {
        assertThat(none.orElseGet(()->2),equalTo(2));
        assertThat(just.orElseGet(()->2),equalTo(10));
    }

    @Test
    public void testToOptional() {
        assertFalse(none.toOptional().isPresent());
        assertTrue(just.toOptional().isPresent());
        assertThat(just.toOptional(),equalTo(Optional.of(10)));
    }

    @Test
    public void testToStream() {
        assertThat(none.stream().collect(Collectors.toList()).size(),equalTo(0));
        assertThat(just.stream().collect(Collectors.toList()).size(),equalTo(1));

    }


    @Test
    public void testOrElse() {
        assertThat(none.orElse(20),equalTo(20));
        assertThat(just.orElse(20),equalTo(10));
    }





    @Test
    public void testIterator1() {
        assertThat(Streams.stream(just.iterator()).collect(Collectors.toList()),
                equalTo(Arrays.asList(10)));
    }

    @Test
    public void testForEach() {
        Mutable<Integer> capture = Mutable.of(null);
         none.forEach(c->capture.set(c));
        assertNull(capture.get());
        just.forEach(c->capture.set(c));
        assertThat(capture.get(),equalTo(10));
    }

    @Test
    public void testSpliterator() {
        assertThat(StreamSupport.stream(just.spliterator(),false).collect(Collectors.toList()),
                equalTo(Arrays.asList(10)));
    }

    @Test
    public void testMapFunctionOfQsuperTQextendsR1() {
        assertThat(just.map(i->i+5),equalTo(LazyEither4.right(15)));
    }

    @Test
    public void testPeek() {
        Mutable<Integer> capture = Mutable.of(null);
        just = just.peek(c->capture.set(c));
        just.get();

        assertThat(capture.get(),equalTo(10));
    }

    private Trampoline<Integer> sum(int times, int sum){
        return times ==0 ?  Trampoline.done(sum) : Trampoline.more(()->sum(times-1,sum+times));
    }


    @Test
    public void testUnitT1() {
        assertThat(none.unit(10),equalTo(just));
    }


}
