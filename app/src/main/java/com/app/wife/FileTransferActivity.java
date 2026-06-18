package com.wife.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wife.app.databinding.ActivityFileTransferBinding;

import java.util.ArrayList;
import java.util.List;

public class FileTransferActivity extends AppCompatActivity implements 
        FileSender.FileTransferListener, 
        FileReceiver.FileReceiveListener,
        FileAdapter.OnFileDeleteListener {

    private ActivityFileTransferBinding binding;
    private FileAdapter adapter;
    private final List<FileEntity> historyList = new ArrayList<>();
    private RoomDatabaseManager db;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onFileSelected
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = RoomDatabaseManager.getInstance(this);

        setupToolbar();
        setupRecyclerView();

        binding.btnPickFile.setOnClickListener(v -> {
            filePickerLauncher.launch("*/*"); // Let user pick any file type
        });

        loadHistory();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarFileTransfer);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarFileTransfer.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new FileAdapter(historyList, this);
        binding.rvFileHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFileHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        List<FileEntity> logs = db.fileDao().getAllFiles();
        historyList.clear();
        historyList.addAll(logs);
        adapter.notifyDataSetChanged();
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) return;

        String filename = "Unknown_File";
        long size = 0;

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                
                if (nameIdx != -1) filename = cursor.getString(nameIdx);
                if (sizeIdx != -1) size = cursor.getLong(sizeIdx);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.layoutTransferProgress.setVisibility(View.VISIBLE);
        binding.tvActiveFileName.setText("Uploading: " + filename);
        binding.pbTransferPercentage.setProgress(0);
        binding.tvTransferPercentText.setText("0%");

        FileSender.getInstance(this).sendFile(uri, filename, size, this);
    }

    // --- Outgoing Callbacks (FileSender.FileTransferListener) ---

    @Override
    public void onProgress(int percent) {
        binding.pbTransferPercentage.setProgress(percent);
        binding.tvTransferPercentText.setText(percent + "%");
    }

    @Override
    public void onComplete(String path) {
        Toast.makeText(this, "File sent successfully!", Toast.LENGTH_SHORT).show();
        binding.layoutTransferProgress.setVisibility(View.GONE);
        loadHistory();
    }

    @Override
    public void onError(String error) {
        Toast.makeText(this, "Transfer failed: " + error, Toast.LENGTH_SHORT).show();
        binding.layoutTransferProgress.setVisibility(View.GONE);
    }

    // --- Incoming Callbacks (FileReceiver.FileReceiveListener) ---

    @Override
    public void onProgress(String filename, int percent) {
        runOnUiThread(() -> {
            binding.layoutTransferProgress.setVisibility(View.VISIBLE);
            binding.tvActiveFileName.setText("Receiving: " + filename);
            binding.pbTransferPercentage.setProgress(percent);
            binding.tvTransferPercentText.setText(percent + "%");
        });
    }

    @Override
    public void onComplete(String filename, String localPath) {
        runOnUiThread(() -> {
            Toast.makeText(this, "File received successfully: " + filename, Toast.LENGTH_LONG).show();
            binding.layoutTransferProgress.setVisibility(View.GONE);
            loadHistory();
        });
    }

    // Callback invoked when delete button is tapped inside RecyclerView row item
    @Override
    public void onFileDelete(FileEntity file, int position) {
        WifeLogger.log("FileTransferActivity", "User requested deletion of file log entity: " + file.getFilename() + " at index: " + position);
        new Thread(() -> {
            try {
                // 1. Delete entity record from SQLite database using the DAO
                db.fileDao().deleteById(file.getId());
                WifeLogger.log("FileTransferActivity", "Successfully deleted file transfer entry from Room Database.");

                // 2. Refresh active list elements on Main UI Thread safely
                runOnUiThread(() -> {
                    try {
                        if (position < historyList.size()) {
                            historyList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, historyList.size());
                            Toast.makeText(this, "File transfer entry removed.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        WifeLogger.log("FileTransferActivity", "Failed updating active adapters during UI update: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                WifeLogger.log("FileTransferActivity", "Error executing file transfer log deletion background thread: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileReceiver.registerListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FileReceiver.unregisterListener(this);
    }
}