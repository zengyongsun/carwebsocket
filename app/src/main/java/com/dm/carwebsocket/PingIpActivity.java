package com.dm.carwebsocket;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PingIpActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = PingIpActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log);
        recyclerView = findViewById(R.id.logList);
        editText = findViewById(R.id.inputIp);
        findViewById(R.id.btPing).setOnClickListener(this);
        adapter = new LogAdapter(LayoutInflater.from(this));
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btPing:
                (new Thread(new LocalThread())).start();
                break;
            default:
                break;
        }
    }

    class LocalThread implements Runnable {

        private String command;

        public void setCommand(String command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("ping -c10 192.168.4.7");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                final StringBuffer output = new StringBuffer();
                int read;
                char[] buff = new char[2048];
                while ((read = bufferedReader.read(buff)) != -1) {
                    output.append(buff, 0, read);
                    Log.d(TAG, Thread.currentThread().getName());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, output.toString());
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("RunAdbActivity", "onViewExecuteClicked: " + e.getMessage());
            }
        }
    }


    public void message(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ShowLogActivity", "run: ShowLogActivity");
                adapter.addItem(new LogBean(System.currentTimeMillis(), new String(str)));
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
    }
}