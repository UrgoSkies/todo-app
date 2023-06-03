package com.example.imtired
// Creator : Rinalds Mackevics 201RDB354
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.content.Context
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.SearchView

class MainActivity : AppCompatActivity() {

    private lateinit var buttonAdd: Button
    private lateinit var listViewTasks: ListView
    private lateinit var searchViewTasks: SearchView
    private lateinit var tasks: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>

    private val fileName = "tasks.txt" // task save val

    private val taskCreationTimesFileName = "task_creation_times.txt" // task time save val
    private val taskDescriptions: HashMap<String, String> = HashMap()

    private val taskLastModifiedTimesFileName = "task_last_modified_times.txt" // task last edit save val
    private val taskLastModifiedTimes: HashMap<String, Long> = HashMap()

    private val taskDescriptionsFileName = "task_descriptions.txt" // task description save
    private val taskCreationTimes: HashMap<String, Long> = HashMap()


    override fun onCreate(savedInstanceState: Bundle?) {
        // this is where you initialize your activity
        super.onCreate(savedInstanceState)
        // this inflates the layout defined in 'activity_main.xml' for this activity.
        setContentView(R.layout.activity_main)

        // initialize the SearchView
        searchViewTasks = findViewById(R.id.searchViewTasks)
        searchViewTasks.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // this method is called when the user submits the search query.
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            // this method is called whenever the user changes the text in the SearchView
            override fun onQueryTextChange(newText: String): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })

        // initialize the add button.
        buttonAdd = findViewById(R.id.buttonAdd)
        // initialize the ListView that will display the tasks
        listViewTasks = findViewById(R.id.listViewTasks)
        tasks = ArrayList()

        // initialize adapter for the ListView. use a simple list item layout provided by Android
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tasks)
        listViewTasks.adapter = adapter

        // set a click listener for the add button
        buttonAdd.setOnClickListener {
            showAddTaskDialog()
        }

        // set a long click listener for the ListView
        listViewTasks.setOnItemLongClickListener { _, _, position, _ ->
            showTaskInfoDialog(position)
            true
        }

        // load tasks from the data source
        tasks = loadTasks()
        // reload adapter with the loaded tasks and set it to ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tasks)
        listViewTasks.adapter = adapter

        // Load creation times , last mod , desc
        loadTaskCreationTimes()
        loadTaskLastModifiedTimes()
        loadTaskDescriptions()
    }


    private fun showTaskInfoDialog(position: Int) {

        // get task at the given position
        val task = tasks[position]
        // get the description of the selected task
        val taskDescription = taskDescriptions[task]

        // dialog view from the layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.task_info_dialog, null)
        val editTextTaskName = dialogView.findViewById<EditText>(R.id.editTextTaskName)
        val editTextTaskDescription = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)

        // edit button from the dialog view
        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonEdit)
        // delete button from the dialog view
        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDelete)

        // set task name and description
        editTextTaskName.setText(task)
        editTextTaskDescription.setText(taskDescription)

        // get and format the creation time of the task
        val creationTime = taskCreationTimes[task]
        val formattedCreationTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(creationTime ?: 0))
        val textViewCreationTime = dialogView.findViewById<TextView>(R.id.textViewCreationTime)
        textViewCreationTime.text = "Created at: $formattedCreationTime"
        // get and format the last modified time of the task
        val lastModifiedTime = taskLastModifiedTimes[task]
        val formattedLastModifiedTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastModifiedTime ?: 0))
        val textViewLastModifiedTime = dialogView.findViewById<TextView>(R.id.textViewLastModifiedTime)
        textViewLastModifiedTime.text = "Last modified at: $formattedLastModifiedTime"

        // title , dialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Task Info")
            .setCancelable(true)
        val dialog = dialogBuilder.create()
        // show dialog
        dialog.show()

        // edit button to update  task name and description, and dismiss the dialog
        buttonEdit.setOnClickListener {
            val newTask = editTextTaskName.text.toString()
            val newTaskDescription = editTextTaskDescription.text.toString()
            updateTask(position, newTask, newTaskDescription)
            dialog.dismiss()
        }
        // delete button to delete task and dismiss the dialog
        buttonDelete.setOnClickListener {
            deleteTask(position)
            dialog.dismiss()
        }
    }


    private fun updateTask(position: Int, newTask: String, newTaskDescription: String) {
        val oldTask = tasks[position]
        val creationTime = taskCreationTimes[oldTask]  // save creation time

        // remove old task times and description
        taskCreationTimes.remove(oldTask)
        taskLastModifiedTimes.remove(oldTask)
        taskDescriptions.remove(oldTask)

        // update task name and description
        tasks[position] = newTask
        taskDescriptions[newTask] = newTaskDescription

        // update times
        taskCreationTimes[newTask] = creationTime ?: System.currentTimeMillis()  // restore creation time
        taskLastModifiedTimes[newTask] = System.currentTimeMillis()
    }



    // TASK ADD DIALOG
    private fun showAddTaskDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setCancelable(true)

        val editTextTask = EditText(this)
        dialogBuilder.setView(editTextTask)

        dialogBuilder.setPositiveButton("Add") { dialog, _ ->
            val task = editTextTask.text.toString()
            tasks.add(task)
            adapter.notifyDataSetChanged()
            val currentTime = System.currentTimeMillis()
            taskCreationTimes[task] = currentTime
            taskLastModifiedTimes[task] = currentTime
            dialog.dismiss()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }





/////SAVE , LOAD , SAVE ON STOP , DELETE/////////////////////////////////////////////////////////////////////////

    private fun saveTasks() {
        try {
            val fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(tasks)
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadTasks(): ArrayList<String> {
        val tasks: ArrayList<String> = ArrayList()
        try {
            val fileInputStream = openFileInput(fileName)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val savedTasks = objectInputStream.readObject()
            if (savedTasks is ArrayList<*>) {
                for (task in savedTasks) {
                    if (task is String) {
                        tasks.add(task)
                    }
                }
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: FileNotFoundException) {
            // file not found,
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return tasks
    }

    override fun onStop() {
        super.onStop()
        saveTasks()
        saveTaskCreationTimes()
        saveTaskLastModifiedTimes()
        saveTaskDescriptions()
    }

    private fun deleteTask(position: Int) {
        val task = tasks[position]
        tasks.removeAt(position)
        adapter.notifyDataSetChanged()
        taskCreationTimes.remove(task)
        taskLastModifiedTimes.remove(task)
        taskDescriptions.remove(task)
    }







/////TASK DESCRIPTION/////////////////////////////////////////////////////////////////////////
    private fun saveTaskDescriptions() {
        try {
            val fileOutputStream = openFileOutput(taskDescriptionsFileName, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(taskDescriptions)
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadTaskDescriptions() {
        try {
            val fileInputStream = openFileInput(taskDescriptionsFileName)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val savedTaskDescriptions = objectInputStream.readObject()
            if (savedTaskDescriptions is HashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                taskDescriptions.putAll(savedTaskDescriptions as HashMap<String, String>)
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: FileNotFoundException) {
            // file not found
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }




/////CREATION TIME/////////////////////////////////////////////////////////////////////////

    private fun saveTaskCreationTimes() {
        try {
            val fileOutputStream = openFileOutput(taskCreationTimesFileName, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(taskCreationTimes)
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadTaskCreationTimes() {
        try {
            val fileInputStream = openFileInput(taskCreationTimesFileName)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val savedTaskCreationTimes = objectInputStream.readObject()
            if (savedTaskCreationTimes is HashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                taskCreationTimes.putAll(savedTaskCreationTimes as HashMap<String, Long>)
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: FileNotFoundException) {
            // file not found
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }





/////LAST MOD TIME/////////////////////////////////////////////////////////////////////////

    private fun loadTaskLastModifiedTimes() {
        try {
            val fileInputStream = openFileInput(taskLastModifiedTimesFileName)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val savedTaskLastModifiedTimes = objectInputStream.readObject()
            if (savedTaskLastModifiedTimes is HashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                taskLastModifiedTimes.putAll(savedTaskLastModifiedTimes as HashMap<String, Long>)
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: FileNotFoundException) {
            // file not found
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }
    private fun saveTaskLastModifiedTimes() {
        try {
            val fileOutputStream = openFileOutput(taskLastModifiedTimesFileName, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(taskLastModifiedTimes)
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }




}