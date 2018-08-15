package com.beerme.beerme;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FriendsListAdapter extends ArrayAdapter<DrinkingFriend> {

   public FriendsListAdapter(Context context, ArrayList<DrinkingFriend> friends){
       super(context,0,friends);
   }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.friends_array_adapter_vew,parent,false);
        }
        TextView name = listItemView.findViewById(R.id.left);
        TextView drinks = listItemView.findViewById(R.id.right);
        DrinkingFriend friend = getItem(position);
        name.setText(friend.getName());
        drinks.setText(""+friend.getDrinks());
        return listItemView;
    }
}
