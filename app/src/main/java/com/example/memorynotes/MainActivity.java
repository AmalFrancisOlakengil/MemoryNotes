package com.example.memorynotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewNotes;
    private Button buttonNewNote;
    private Button buttonDeleteNotes;
    private NotesAdapter notesAdapter;
    private ArrayList<Note> notesList;
    private NotesDatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = new NotesDatabaseManager(this);

        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        buttonNewNote = findViewById(R.id.buttonNewNote);
        buttonDeleteNotes = findViewById(R.id.buttonDeleteNotes);

        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        notesList = dbManager.getAllNotes();
        notesAdapter = new NotesAdapter(notesList);
        recyclerViewNotes.setAdapter(notesAdapter);

        buttonNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewNoteDialog();
            }
        });

        buttonDeleteNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedNotes();
            }
        });
    }

    private void showNewNoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Note");

        final EditText input = new EditText(this);
        input.setHint("Enter note text");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String noteText = input.getText().toString().trim();
            if (!noteText.isEmpty()) {
                addNoteToDatabase(noteText);
            } else {
                Toast.makeText(MainActivity.this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addNoteToDatabase(String noteText) {
        dbManager.addNote(noteText);
        notesList.clear();
        notesList.addAll(dbManager.getAllNotes());
        notesAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility();
    }

    private void deleteSelectedNotes() {
        ArrayList<Note> notesToRemove = new ArrayList<>();
        for (Note note : notesList) {
            if (note.isSelected()) {
                dbManager.deleteNote(note.getId());
                notesToRemove.add(note);
            }
        }
        notesList.removeAll(notesToRemove);
        notesAdapter.notifyDataSetChanged();
        updateRecyclerViewVisibility();
    }

    private void updateRecyclerViewVisibility() {
        if (notesList.isEmpty()) {
            recyclerViewNotes.setVisibility(View.GONE);
        } else {
            recyclerViewNotes.setVisibility(View.VISIBLE);
        }
    }

    public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

        private ArrayList<Note> notesList;

        public NotesAdapter(ArrayList<Note> notesList) {
            this.notesList = notesList;
        }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = notesList.get(position);
            holder.textViewNote.setText(note.getText());
            holder.checkBox.setChecked(note.isSelected());
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> note.setSelected(isChecked));
        }

        @Override
        public int getItemCount() {
            return notesList.size();
        }

        public class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView textViewNote;
            CheckBox checkBox;

            public NoteViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewNote = itemView.findViewById(R.id.textViewNote);
                checkBox = itemView.findViewById(R.id.checkBox);
            }
        }
    }

    public class Note {
        private int id;
        private String text;
        private boolean selected;

        public Note(int id, String text) {
            this.id = id;
            this.text = text;
            this.selected = false;
        }

        public int getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
    public class NotesDatabaseManager {

        private DatabaseHelper dbHelper;
        private SQLiteDatabase database;

        public NotesDatabaseManager(Context context) {
            dbHelper = new DatabaseHelper(context);
            database = dbHelper.getWritableDatabase();
        }

        public void addNote(String noteText) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_NOTE_TEXT, noteText);
            database.insert(DatabaseHelper.TABLE_NOTES, null, values);
        }

        public ArrayList<Note> getAllNotes() {
            ArrayList<Note> notes = new ArrayList<>();
            Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES,
                    new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_NOTE_TEXT},
                    null, null, null, null, null);

            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int noteTextIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTE_TEXT);
                
                if (idIndex >= 0 && noteTextIndex >= 0) {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(idIndex);
                        String noteText = cursor.getString(noteTextIndex);
                        notes.add(new Note(id, noteText));
                    }
                } else {
                    throw new IllegalStateException("Database columns not found");
                }

                cursor.close();
            }
            return notes;
        }

        public void deleteNote(int id) {
            database.delete(DatabaseHelper.TABLE_NOTES, DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(id)});
        }
    }

}

