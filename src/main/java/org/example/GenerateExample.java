package org.example;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.generator.Generator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();

        Example example = gen.generateValueOfType(Example.class);
        int[] numbers = gen.generateValueOfType(int[].class);
        List<?> items = gen.generateValueOfType(List.class);
        Map<?, ?> entries = gen.generateValueOfType(Map.class);
        Shape shape = gen.generateValueOfType(Shape.class);
        Rectangle[] rectangles = gen.generateValueOfType(Rectangle[].class);
        Cart cart = gen.generateValueOfType(Cart.class);
        BinaryTreeNode tree = gen.generateValueOfType(BinaryTreeNode.class);

        System.out.println("Example: " + example);
        System.out.println("int[]: " + Arrays.toString(numbers));
        System.out.println("List: " + items);
        System.out.println("Map: " + entries);
        System.out.printf("Shape: %s, area = %.2f, perimeter = %.2f%n",
                shape.getClass().getSimpleName(), shape.getArea(), shape.getPerimeter());
        System.out.println("Rectangle[] length: " + rectangles.length);
        System.out.println("Cart items: " + cart.getItems().size());
        System.out.println("Tree root data: " + tree.getData());
    }
}
