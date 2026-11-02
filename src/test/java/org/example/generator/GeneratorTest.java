package org.example.generator;

import org.example.classes.*;
import org.example.model.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    private static final int RUNS = 200;

    private final Generator generator = new Generator(new Random(42));

    private static <T> Set<T> collect(Supplier<T> supplier) {
        Set<T> values = new HashSet<>();
        IntStream.range(0, RUNS).forEach(_ -> values.add(supplier.get()));
        return values;
    }

    private static Type fieldType(String name) throws NoSuchFieldException {
        return Holder.class.getField(name).getGenericType();
    }

    @Nested
    class BuiltInTypes {
        @ParameterizedTest
        @ValueSource(classes = {int.class, long.class, short.class, byte.class, double.class, float.class,
                boolean.class, char.class, Integer.class, Long.class, Short.class, Byte.class,
                Double.class, Float.class, Boolean.class, Character.class})
        void primitivesAndWrappers(Class<?> type) {
            Object value = generator.generateValueOfType(type);
            assertNotNull(value);
            Class<?> expected = type.isPrimitive() ? java.lang.invoke.MethodType.methodType(type).wrap().returnType() : type;
            assertInstanceOf(expected, value);
        }

        @Test
        void valuesAreRandom() {
            assertTrue(collect(() -> generator.generateValueOfType(int.class)).size() > 1);
            assertTrue(collect(() -> generator.generateValueOfType(double.class)).size() > 1);
            assertEquals(Set.of(true, false), collect(() -> generator.generateValueOfType(boolean.class)));
        }

        @Test
        void strings() {
            Set<String> strings = collect(() -> generator.generateValueOfType(String.class));
            assertTrue(strings.size() > 1);
            strings.forEach(s -> assertFalse(s.isEmpty()));
        }

        @Test
        void enumsCoverAllConstants() {
            assertEquals(Set.of(Color.values()), collect(() -> generator.generateValueOfType(Color.class)));
        }

        @Test
        void arrays() {
            int[] ints = generator.generateValueOfType(int[].class);
            assertTrue(ints.length > 0);

            String[] strings = generator.generateValueOfType(String[].class);
            assertTrue(strings.length > 0);
            for (String s : strings) {
                assertNotNull(s);
            }

            Color[] colors = generator.generateValueOfType(Color[].class);
            assertTrue(colors.length > 0);

            Rectangle[] rectangles = generator.generateValueOfType(Rectangle[].class);
            assertTrue(rectangles.length > 0);
            for (Rectangle r : rectangles) {
                assertTrue(r.getArea() > 0);
            }
        }

        @Test
        void rawListIsNonEmpty() {
            List<?> list = generator.generateValueOfType(List.class);
            assertFalse(list.isEmpty());
            list.forEach(e -> assertInstanceOf(String.class, e));
        }

        @Test
        void rawMapIsNonEmpty() {
            Map<?, ?> map = generator.generateValueOfType(Map.class);
            assertFalse(map.isEmpty());
            map.forEach((k, v) -> {
                assertInstanceOf(String.class, k);
                assertInstanceOf(String.class, v);
            });
        }

        @Test
        void genericListUsesElementType() throws NoSuchFieldException {
            List<?> list = (List<?>) generator.generateValueOfType(fieldType("integers"));
            assertFalse(list.isEmpty());
            list.forEach(e -> assertInstanceOf(Integer.class, e));
        }

        @Test
        void nestedGenericMap() throws NoSuchFieldException {
            Map<?, ?> map = (Map<?, ?>) generator.generateValueOfType(fieldType("nested"));
            assertFalse(map.isEmpty());
            map.forEach((k, v) -> {
                assertInstanceOf(Color.class, k);
                List<?> values = assertInstanceOf(List.class, v);
                assertFalse(values.isEmpty());
                values.forEach(d -> assertInstanceOf(Double.class, d));
            });
        }

        @Test
        void wildcardUsesUpperBound() throws NoSuchFieldException {
            List<?> animals = (List<?>) generator.generateValueOfType(fieldType("animals"));
            assertFalse(animals.isEmpty());
            animals.forEach(a -> assertInstanceOf(Animal.class, a));
        }

        @Test
        void sameSeedGivesSameResult() {
            int[] first = new Generator(new Random(7)).generateValueOfType(int[].class);
            int[] second = new Generator(new Random(7)).generateValueOfType(int[].class);
            assertArrayEquals(first, second);
        }
    }

    @Nested
    class UserObjects {
        @Test
        void templateClasses() {
            assertNotNull(generator.generateValueOfType(Example.class));
            assertNotNull(generator.generateValueOfType(Product.class).getName());
            assertTrue(generator.generateValueOfType(Rectangle.class).getArea() > 0);
        }

        @Test
        void cartContainsProducts() {
            Cart cart = generator.generateValueOfType(Cart.class);
            assertFalse(cart.getItems().isEmpty());
            cart.getItems().forEach(p -> {
                assertInstanceOf(Product.class, p);
                assertNotNull(p.getName());
                assertTrue(p.getPrice() > 0);
            });
        }

        @Test
        void recursiveTreeIsBoundedByMaxDepth() {
            int maxDepth = 3;
            BinaryTreeNode root = new Generator(new Random(1), maxDepth).generateValueOfType(BinaryTreeNode.class);
            assertNotNull(root.getData());
            assertEquals(maxDepth, depth(root));
        }

        private int depth(BinaryTreeNode node) {
            return node == null ? 0 : 1 + Math.max(depth(node.getLeft()), depth(node.getRight()));
        }

        @Test
        void recordIsSupported() {
            assertNotNull(generator.generateValueOfType(Point.class));
        }

        @Test
        void objectWithSetAndMapOfObjects() {
            Person person = generator.generateValueOfType(Person.class);
            assertFalse(person.getFavouriteColors().isEmpty());
            assertFalse(person.getPlaces().isEmpty());
            person.getPlaces().values().forEach(p -> assertInstanceOf(Point.class, p));
        }

        @Test
        void shapeGivesAllAnnotatedImplementations() {
            Set<Class<?>> classes = collect(() -> generator.generateValueOfType(Shape.class).getClass());
            assertEquals(Set.of(Rectangle.class, Triangle.class), classes);
        }

        @Test
        void abstractClassGivesOnlyAnnotatedSubclasses() {
            Set<Class<?>> classes = collect(() -> generator.generateValueOfType(Animal.class).getClass());
            assertEquals(Set.of(Dog.class, Cat.class), classes);
            assertFalse(classes.contains(Fish.class));
        }
    }

    @Nested
    class GeneratableSemantics {
        @Test
        void annotatedConstructorIsPreferredEvenIfPrivate() {
            for (int i = 0; i < 50; i++) {
                assertTrue(generator.generateValueOfType(Person.class).isCreatedByAnnotatedConstructor());
            }
        }

        @Test
        void classWithoutAnnotationIsRejected() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(NotGeneratable.class));
            assertTrue(e.getMessage().contains("not annotated with @Generatable"));
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Fish.class));
        }

        @Test
        void nestedUnannotatedDependencyIsReportedWithContext() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(Wrapper.class));
            assertTrue(e.getMessage().contains("parameter 'inner'"));
            assertTrue(e.getMessage().contains(NotGeneratable.class.getName()));
        }

        @Test
        void interfaceWithoutAnnotatedImplementationsIsRejected() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(Vehicle.class));
            assertTrue(e.getMessage().contains("No @Generatable implementations"));
        }

        @Test
        void classWithOnlyPrivateUnannotatedConstructorIsRejected() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(PrivateOnly.class));
            assertTrue(e.getMessage().contains("no public constructors"));
        }
    }

    @Nested
    class Failures {
        @Test
        void voidIsRejected() {
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(void.class));
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Void.class));
        }

        @Test
        void objectIsRejected() {
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Object.class));
        }

        @Test
        void emptyEnumIsRejected() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(EmptyEnum.class));
            assertTrue(e.getMessage().contains("no constants"));
        }

        @Test
        void constructorThatAlwaysThrowsIsReportedWithCause() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(AlwaysFails.class));
            assertInstanceOf(IllegalStateException.class, e.getCause());
        }

        @Test
        void unsatisfiableInvariantIsReported() {
            var e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(Impossible.class));
            assertTrue(e.getMessage().contains("@Invariant"));
        }

        @Test
        void invalidMaxDepth() {
            assertThrows(IllegalArgumentException.class, () -> new Generator(new Random(), 0));
        }
    }

    @Nested
    class DomainConstraints {
        @Test
        void rangeIsRespected() {
            for (int i = 0; i < RUNS; i++) {
                Temperature t = generator.generateValueOfType(Temperature.class);
                assertTrue(t.getCelsius() >= -50 && t.getCelsius() <= 50);
                assertEquals(2732, t.getKelvinTimesTen());
            }
        }

        @Test
        void rectangleHasPositiveSides() {
            for (int i = 0; i < RUNS; i++) {
                Rectangle r = generator.generateValueOfType(Rectangle.class);
                assertTrue(r.getArea() > 0);
                assertTrue(r.getPerimeter() > 0);
            }
        }

        @Test
        void triangleIsAlwaysValid() {
            for (int i = 0; i < RUNS; i++) {
                Triangle t = generator.generateValueOfType(Triangle.class);
                double area = t.getArea();
                assertFalse(Double.isNaN(area));
                assertTrue(area > 0);
            }
        }

        @Test
        void constructorRejectionLeadsToRetry() {
            for (int i = 0; i < 50; i++) {
                assertTrue(generator.generateValueOfType(PositiveOnly.class).getValue() > 0);
            }
        }
    }
}
