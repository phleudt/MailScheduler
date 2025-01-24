package com.mailscheduler.service;

import java.util.Scanner;
import java.util.List;

public abstract class AbstractUserConsoleInteractionService {
    protected final Scanner scanner;

    public AbstractUserConsoleInteractionService() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Validates that an input is within the valid range of drafts/options.
     *
     * @param numberOfOptions Total number of available options
     * @param input User's selected input
     * @return true if input is valid, false otherwise
     */
    protected boolean validateInput(int numberOfOptions, int input) {
        return numberOfOptions >= input && input > 0;
    }

    /**
     * Validates multiple inputs against the total number of options.
     *
     * @param numberOfOptions Total number of available options
     * @param inputs List of user's selected inputs (zero-indexed)
     * @return true if all inputs are valid, false otherwise
     */
    protected boolean validateInputs(int numberOfOptions, List<Integer> inputs) {
        for (Integer input : inputs) {
            if (numberOfOptions <= input || input < 0) {
                System.out.println("Input " + input + " is invalid.");
                return false;
            }
        }
        return true;
    }

    /**
     * Reads and validates integer input from the user.
     *
     * @param prompt Message to display to the user
     * @param numberOfOptions Total number of available options
     * @return Validated user input
     */
    protected int getValidatedIntegerInput(String prompt, int numberOfOptions) {
        while (true) {
            System.out.println(prompt);
            String input = scanner.next();
            try {
                int choice = Integer.parseInt(input);
                if (validateInput(numberOfOptions, choice)) {
                    return choice;
                } else {
                    System.out.println("Invalid input. Please enter a number between 1 and " + numberOfOptions);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number");
            }
        }
    }

    /**
     * Reads and validates multiple integer inputs from the user.
     *
     * @param prompt Message to display to the user
     * @param numberOfOptions Total number of available options
     * @return List of validated user inputs
     */
    protected List<Integer> getValidatedMultipleInputs(String prompt, int numberOfOptions) {
        while (true) {
            System.out.println(prompt);
            String input = scanner.next();
            try {
                List<Integer> choices = java.util.Arrays.stream(input.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList();
                if (validateInputs(numberOfOptions, choices)) {
                    return choices;
                } else {
                    System.out.println("Invalid input. Please enter numbers between 1 and " + numberOfOptions);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a comma-separated list of numbers.");
            }
        }
    }
}