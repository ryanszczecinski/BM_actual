package com.beerme.beerme;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class FriendRequestAdapter extends ArrayAdapter<String> {
    public FriendRequestAdapter(Context context, ArrayList<String> friendRequests){
        super(context,0,friendRequests);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.friend_request_adapter_view,parent,false);
        }
        TextView name = listItemView.findViewById(R.id.FRTV);
        name.setText(getItem(position));
        ImageButton acceptFriend = listItemView.findViewById(R.id.FRaddFriend);
        acceptFriend.setTag(getItem(position));
        ImageButton deleteRequest = listItemView.findViewById(R.id.FRdeleteRequest);
        deleteRequest.setTag(getItem(position));
        View.OnClickListener addFriendListner = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // TODO: 8/15/2018 do shit in data base to add a friend and delete view
                //first try to get the view removed then worry about DB\
                //have to delete the string from the backing array as well...line below may do everything we need
                remove((String)view.getTag());
            }
        };
        View.OnClickListener deleteRequestListner = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // TODO: 8/15/2018 do shit in data base to remove request and delete view
                //first try to get the view removed then worry about DB
                remove((String)view.getTag());
            }
        };
        acceptFriend.setOnClickListener(addFriendListner);
        deleteRequest.setOnClickListener(deleteRequestListner);
        return listItemView;
    }
}
