package com.beerme.beerme;

public class DrinkingFriend {
    private String name;
    private int numberOfDrinks;
    public DrinkingFriend(String n, int num){
        name = n;
        numberOfDrinks= num;
    }
    public String getName(){
        return name;
    }
    public int getDrinks(){
        return numberOfDrinks;
    }
    public void setName(String n){
        name = n;
    }
    public void setNumberOfDrinks(int num){
        numberOfDrinks= num;
    }
}
