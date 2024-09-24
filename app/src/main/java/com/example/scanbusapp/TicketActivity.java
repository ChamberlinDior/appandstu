package com.example.scanbusapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TicketActivity extends AppCompatActivity {

    private static final String TAG = "TicketActivity";
    private Button printTicketButton;
    private ImageView ticketImageView;
    private TextView ticketDetailsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        // Initialize views
        printTicketButton = findViewById(R.id.print_ticket_button);
        ticketImageView = findViewById(R.id.ticket_image_view);  // ImageView for ticket image
        ticketDetailsView = findViewById(R.id.ticket_details_view);

        // Retrieve ticket details from intent
        Intent intent = getIntent();
        String ticketDetails = intent.getStringExtra("ticketDetails");

        // Display ticket details (optional text view)
        ticketDetailsView.setText(ticketDetails);

        // Load ticket image
        ticketImageView.setImageResource(R.drawable.active_image);  // Correct image reference

        // Print or save the ticket
        printTicketButton.setOnClickListener(v -> generateAndPrintTicket(ticketDetails));
    }

    // Method to generate and print/save the ticket
    private void generateAndPrintTicket(String ticketContent) {
        File ticketFile = new File(getExternalFilesDir(null), "ticket_bus.txt");
        try (FileOutputStream fos = new FileOutputStream(ticketFile)) {
            fos.write(ticketContent.getBytes());
            fos.flush();
            printTicket(ticketFile);
        } catch (IOException e) {
            Log.e(TAG, "Error generating the ticket", e);
            Toast.makeText(this, "Error generating the ticket", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to print the ticket (or save it if no printer available)
    private void printTicket(File ticketFile) {
        Intent printIntent = new Intent(Intent.ACTION_VIEW);
        printIntent.setDataAndType(Uri.fromFile(ticketFile), "text/plain");
        printIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        // Check if any app can handle the print intent
        if (printIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(printIntent);
        } else {
            Toast.makeText(this, "No printer app available. Ticket saved locally.", Toast.LENGTH_SHORT).show();
        }
    }
}
