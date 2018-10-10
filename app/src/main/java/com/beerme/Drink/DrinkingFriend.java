package com.beerme.Drink;




public class DrinkingFriend {
    private String name;
    private int numberOfDrinks;
    private boolean isDrinking;


    public DrinkingFriend(String n, boolean drinking,int num){
        name = n;
        numberOfDrinks= num;
        isDrinking = drinking;
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
    public void setNumberOfDrinks(int num){numberOfDrinks= num;}

    public boolean isDrinking() {
        return isDrinking;
    }

    public void setIsDrinking(boolean drinking) {
        isDrinking = drinking;
    }


    @Override
    public boolean equals(Object other) {
      if(other != null&& other instanceof DrinkingFriend) {
            return name.equals(((DrinkingFriend) other).getName());
      }
      return false;
    }
}
