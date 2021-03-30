package com.dm.carwebsocket;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.carwebsocket.gps.ClientSocket;

import java.util.ArrayList;
import java.util.List;

public class ShowLogActivity extends AppCompatActivity implements ClientSocket.ConnectState {

    private RecyclerView recyclerView;
    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log);
        recyclerView = findViewById(R.id.logList);
        adapter = new LogAdapter(LayoutInflater.from(this));
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        ClientSocket.getInstance().setState(this);
    }


    @Override
    public void socketDisconnect() {

    }

    @Override
    public void message(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowLogActivity", "run: ShowLogActivity");
                adapter.addItem(new LogBean(System.currentTimeMillis(), str));
            }
        });
    }


    private static class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {

        private LayoutInflater inflater;
        private List<LogBean> logs = new ArrayList<>();

        public LogAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        public void addItem(LogBean logItem) {
            logs.add(logItem);
            notifyItemInserted(logs.size() - 1);
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = inflater.inflate(R.layout.adapter_log_layout, parent, false);
            return new LogViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogBean logItem = logs.get(position);
            holder.messageView.setText(logItem.flattenedLog());
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }
    }

    private static class LogViewHolder extends RecyclerView.ViewHolder {

        private TextView messageView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            messageView = itemView.findViewById(R.id.message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ClientSocket.getInstance().removeState(this);
    }
}