package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Generator {
    public static final int DEFAULT_MAX_DEPTH = 4;

    private static final int MAX_ATTEMPTS = 100;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 5;
    private static final int MAX_STRING_LENGTH = 10;
    private static final double DEFAULT_MIN = -1000;
    private static final double DEFAULT_MAX = 1000;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
            Boolean.class, boolean.class,
            Character.class, char.class,
            Byte.class, byte.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class
    );

    private final Random random;
    private final int maxDepth;
    private final ImplementationScanner scanner;

    public Generator() {
        this(new Random());
    }

    public Generator(Random random) {
        this(random, DEFAULT_MAX_DEPTH);
    }

    /**
     * @param maxDepth      максимальная глубина вложенности пользовательских объектов
     * @param scanPackages  дополнительные пакеты для поиска реализаций интерфейсов и абстрактных классов
     */
    public Generator(Random random, int maxDepth, String... scanPackages) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be positive, got " + maxDepth);
        }
        this.random = random;
        this.maxDepth = maxDepth;
        this.scanner = new ImplementationScanner(Set.of(scanPackages));
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> clazz) {
        return (T) generateValueOfType((Type) clazz);
    }

    public Object generateValueOfType(Type type) {
        return generate(type, null, 0);
    }

    private Object generate(Type type, Range range, int depth) {
        if (type instanceof Class<?> clazz) {
            return generateClass(clazz, new Type[0], range, depth);
        }
        if (type instanceof ParameterizedType parameterized) {
            return generateClass((Class<?>) parameterized.getRawType(), parameterized.getActualTypeArguments(), range, depth);
        }
        if (type instanceof GenericArrayType arrayType) {
            return generateArray(arrayType.getGenericComponentType(), depth);
        }
        if (type instanceof WildcardType || type instanceof TypeVariable<?>) {
            return generate(typeArgument(new Type[]{type}, 0), range, depth);
        }
        throw new GenerationException("Unsupported type: " + type);
    }

    private Object generateClass(Class<?> clazz, Type[] typeArgs, Range range, int depth) {
        if (clazz == void.class || clazz == Void.class) {
            throw new GenerationException("Cannot generate a value of type void");
        }
        if (clazz.isPrimitive() || WRAPPER_TO_PRIMITIVE.containsKey(clazz)) {
            return generatePrimitive(clazz.isPrimitive() ? clazz : WRAPPER_TO_PRIMITIVE.get(clazz), range);
        }
        if (range != null) {
            throw new GenerationException("@Range is applicable only to numeric types, but used on " + clazz.getName());
        }
        if (clazz == String.class) {
            return randomString();
        }
        if (clazz.isEnum()) {
            return generateEnum(clazz);
        }
        if (clazz.isArray()) {
            return generateArray(clazz.getComponentType(), depth);
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            return generateCollection(clazz, typeArgument(typeArgs, 0), depth);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return generateMap(clazz, typeArgument(typeArgs, 0), typeArgument(typeArgs, 1), depth);
        }
        if (depth >= maxDepth) {
            return null;
        }
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return generateImplementation(clazz, depth);
        }
        return generateObject(clazz, depth);
    }

    // ---------- Примитивы, строки, enum ----------

    private Object generatePrimitive(Class<?> type, Range range) {
        if (type == boolean.class || type == char.class) {
            if (range != null) {
                throw new GenerationException("@Range is applicable only to numeric types, but used on " + type.getName());
            }
            return type == boolean.class
                    ? random.nextBoolean()
                    : ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        double min = range != null ? range.min() : DEFAULT_MIN;
        double max = range != null ? range.max() : DEFAULT_MAX;
        if (min > max) {
            throw new GenerationException("Invalid @Range: min " + min + " > max " + max);
        }
        if (type == double.class) {
            return randomDouble(min, max);
        }
        if (type == float.class) {
            return (float) randomDouble(min, max);
        }
        if (type == byte.class) {
            return (byte) randomLong(min, max, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }
        if (type == short.class) {
            return (short) randomLong(min, max, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        if (type == int.class) {
            return (int) randomLong(min, max, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return randomLong(min, max, Long.MIN_VALUE, Long.MAX_VALUE - 1);
    }

    private double randomDouble(double min, double max) {
        return min == max ? min : random.nextDouble(min, max);
    }

    private long randomLong(double min, double max, long typeMin, long typeMax) {
        long lo = (long) Math.max(Math.ceil(min), typeMin);
        long hi = (long) Math.min(Math.floor(max), typeMax);
        if (lo > hi) {
            throw new GenerationException("@Range [" + min + ", " + max + "] contains no integer values of the target type");
        }
        return random.nextLong(lo, hi + 1);
    }

    private String randomString() {
        int length = random.nextInt(1, MAX_STRING_LENGTH + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private Object generateEnum(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants.length == 0) {
            throw new GenerationException("Enum " + enumClass.getName() + " has no constants");
        }
        return constants[random.nextInt(constants.length)];
    }

    // ---------- Массивы и коллекции ----------

    private int containerSize(int depth) {
        return depth >= maxDepth ? 0 : random.nextInt(MIN_SIZE, MAX_SIZE + 1);
    }

    private Object generateArray(Type componentType, int depth) {
        int length = containerSize(depth);
        Object array = Array.newInstance(rawClass(componentType), length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, generate(componentType, null, depth));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> generateCollection(Class<?> type, Type elementType, int depth) {
        Collection<Object> collection;
        if (type.isAssignableFrom(ArrayList.class)) {
            collection = new ArrayList<>();
        } else if (type.isAssignableFrom(HashSet.class)) {
            collection = new HashSet<>();
        } else if (type.isAssignableFrom(LinkedList.class)) {
            collection = new LinkedList<>();
        } else {
            collection = (Collection<Object>) instantiateContainer(type);
        }
        int size = containerSize(depth);
        // Для множеств возможны повторы, поэтому число попыток ограничено
        for (int attempt = 0; collection.size() < size && attempt < size * 10; attempt++) {
            collection.add(generate(elementType, null, depth));
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> generateMap(Class<?> type, Type keyType, Type valueType, int depth) {
        Map<Object, Object> map = type.isAssignableFrom(HashMap.class)
                ? new HashMap<>()
                : (Map<Object, Object>) instantiateContainer(type);
        int size = containerSize(depth);
        for (int attempt = 0; map.size() < size && attempt < size * 10; attempt++) {
            map.put(generate(keyType, null, depth), generate(valueType, null, depth));
        }
        return map;
    }

    private Object instantiateContainer(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new GenerationException("Unsupported container type " + type.getName()
                    + ": supported are List, Set, Collection, Queue, Deque, Map and concrete containers with a no-arg constructor");
        }
        try {
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new GenerationException("Container " + type.getName() + " has no accessible no-arg constructor", e);
        }
    }

    // ---------- Пользовательские типы ----------

    private Object generateImplementation(Class<?> abstractType, int depth) {
        List<Class<?>> implementations = scanner.findImplementations(abstractType);
        if (implementations.isEmpty()) {
            throw new GenerationException("No @Generatable implementations of " + abstractType.getName()
                    + " found in packages " + scanner.packagesFor(abstractType));
        }
        Class<?> implementation = implementations.get(random.nextInt(implementations.size()));
        return generateClass(implementation, new Type[0], null, depth);
    }

    private Object generateObject(Class<?> clazz, int depth) {
        if (!clazz.isAnnotationPresent(Generatable.class)) {
            throw new GenerationException("Class " + clazz.getName() + " is not annotated with @Generatable");
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new GenerationException("Non-static inner class " + clazz.getName() + " is not supported");
        }
        List<Constructor<?>> constructors = selectConstructors(clazz);
        List<Method> invariants = findInvariants(clazz);

        Throwable lastFailure = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Constructor<?> constructor = constructors.get(random.nextInt(constructors.size()));
            Object[] args = generateArguments(constructor, depth + 1);
            Object instance;
            try {
                instance = constructor.newInstance(args);
            } catch (InvocationTargetException e) {
                lastFailure = e.getCause();
                continue;
            } catch (ReflectiveOperationException e) {
                throw new GenerationException("Cannot instantiate " + clazz.getName(), e);
            }
            if (satisfiesInvariants(instance, invariants)) {
                return instance;
            }
            lastFailure = null;
        }
        String reason = lastFailure != null
                ? "constructor kept throwing " + lastFailure
                : "@Invariant was never satisfied";
        throw new GenerationException("Could not generate a valid " + clazz.getName()
                + " in " + MAX_ATTEMPTS + " attempts: " + reason, lastFailure);
    }

    private List<Constructor<?>> selectConstructors(Class<?> clazz) {
        Constructor<?>[] declared = clazz.getDeclaredConstructors();
        List<Constructor<?>> candidates = Arrays.stream(declared)
                .filter(c -> c.isAnnotationPresent(Generatable.class))
                .toList();
        if (candidates.isEmpty()) {
            candidates = Arrays.stream(declared)
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .toList();
        }
        if (candidates.isEmpty()) {
            throw new GenerationException("Class " + clazz.getName()
                    + " has no public constructors and no constructors annotated with @Generatable");
        }
        candidates.forEach(c -> c.setAccessible(true));
        return candidates;
    }

    private Object[] generateArguments(Constructor<?> constructor, int depth) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            try {
                args[i] = generate(parameter.getParameterizedType(), parameter.getAnnotation(Range.class), depth);
            } catch (GenerationException e) {
                throw new GenerationException("Cannot generate parameter '" + parameter.getName()
                        + "' of " + constructor.getDeclaringClass().getName() + ": " + e.getMessage(), e);
            }
        }
        return args;
    }

    private static List<Method> findInvariants(Class<?> clazz) {
        List<Method> invariants = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Invariant.class)) {
                    continue;
                }
                if (method.getParameterCount() != 0 || method.getReturnType() != boolean.class) {
                    throw new GenerationException("@Invariant method " + c.getName() + "." + method.getName()
                            + " must take no parameters and return boolean");
                }
                method.setAccessible(true);
                invariants.add(method);
            }
        }
        return invariants;
    }

    private static boolean satisfiesInvariants(Object instance, List<Method> invariants) {
        for (Method invariant : invariants) {
            try {
                if (!(boolean) invariant.invoke(instance)) {
                    return false;
                }
            } catch (ReflectiveOperationException e) {
                throw new GenerationException("Failed to check @Invariant " + invariant.getName(), e);
            }
        }
        return true;
    }

    // ---------- Работа с типами ----------

    /**
     * Разрешает неопределенные типы дженериков (Wildcard или TypeVariable) до конкретного класса.
     * Извлекает объявленные границы типа, а при их отсутствии возвращает {@link String String.class}
     * в качестве безопасной заглушки вместо неинстанцируемого {@link Object Object.class}.
     */
    private static Type typeArgument(Type[] typeArgs, int index) {
        if (index >= typeArgs.length) {
            return String.class;
        }
        Type arg = typeArgs[index];
        if (arg instanceof WildcardType wildcard) {
            arg = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
        } else if (arg instanceof TypeVariable<?> variable) {
            arg = variable.getBounds()[0];
        }
        return arg == Object.class ? String.class : arg;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        if (type instanceof GenericArrayType arrayType) {
            return rawClass(arrayType.getGenericComponentType()).arrayType();
        }
        if (type instanceof WildcardType || type instanceof TypeVariable<?>) {
            return rawClass(typeArgument(new Type[]{type}, 0));
        }
        throw new GenerationException("Unsupported type: " + type);
    }
}
