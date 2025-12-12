package com.starter.springboot.java;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

public class ApacheUtils {
    public static void main(String[] args) {

        // 1️⃣ ArrayUtils → Manage product IDs
        int[] productIds = {101, 102, 103};
        System.out.println("Product IDs : " + ArrayUtils.toString(productIds));// products
        productIds = ArrayUtils.add(productIds, 104);
        System.out.println("Product IDs added: " + ArrayUtils.toString(productIds));// Add new product
        productIds = ArrayUtils.remove(productIds, 1);// Remove ID at index 1
        System.out.println("Product IDs removed: " + ArrayUtils.toString(productIds));
        System.out.println("Has product 103? " + ArrayUtils.contains(productIds, 103));

        // 2️⃣ ListUtils → Manage customer wishlists
        List<String> wishlistA = Arrays.asList("Laptop", "Phone", "Mouse");
        List<String> wishlistB = Arrays.asList("Phone", "Tablet", "Camera");

        List<String> allWishes = ListUtils.union(wishlistA, wishlistB);
        List<String> commonWishes = ListUtils.intersection(wishlistA, wishlistB);
        List<String> exclusiveToA = ListUtils.subtract(wishlistA, wishlistB);
        System.out.println("\nAll Wishes: " + allWishes);
        System.out.println("Common: " + commonWishes);
        System.out.println("Only in A: " + exclusiveToA);

        // Split wishlist into chunks (for batch processing)
        System.out.println("Wishlist in chunks: " + ListUtils.partition(allWishes, 2));

        // 3️⃣ SetUtils → Handle product categories (unique)
        Set<String> electronics = new HashSet<>(Arrays.asList("Laptop", "Phone", "Camera"));
        Set<String> home = new HashSet<>(Arrays.asList("Fridge", "TV", "Camera"));

        Set<String> allCategories = SetUtils.union(electronics, home);
        Set<String> commonCategories = SetUtils.intersection(electronics, home);
        Set<String> uniqueToElectronics = SetUtils.difference(electronics, home);
        System.out.println("\nAll Categories: " + allCategories);
        System.out.println("Common Categories: " + commonCategories);
        System.out.println("Unique to Electronics: " + uniqueToElectronics);

        // 4️⃣ CollectionUtils → Work with any type of collection
        List<String> cartItems = Arrays.asList("Phone", "Charger", "Cover");
        List<String> available = Arrays.asList("Phone", "Laptop", "Cover", "Tablet");

        boolean validCart = CollectionUtils.containsAll(available, cartItems);
        System.out.println("\nAll items available? " + validCart);

        // Merge cart and wishlist (remove duplicates)
        System.out.println("Cart + Wishlist (Union): " + CollectionUtils.union(cartItems, wishlistA));

        // 5️⃣ MapUtils → Handle discount configurations
        Map<String, Integer> discounts = new HashMap<>();
        discounts.put("Laptop", 10);
        discounts.put("Phone", 5);

        System.out.println("\nIs Discount Map Empty? " + MapUtils.isEmpty(discounts));
        System.out.println("Laptop Discount: " + MapUtils.getInteger(discounts, "Laptop", 0) + "%");
        System.out.println("TV Discount: " + MapUtils.getInteger(discounts, "TV", 0) + "%");

        // Null-safe map example
        Map<String, Integer> nullMap = null;
        System.out.println("Safe Empty Map: " + MapUtils.emptyIfNull(nullMap));

        // Make it read-only (to prevent modification)
        Map<String, Integer> unmodifiable = MapUtils.unmodifiableMap(discounts);
        System.out.println("Unmodifiable Discounts: " + unmodifiable);
    }
}