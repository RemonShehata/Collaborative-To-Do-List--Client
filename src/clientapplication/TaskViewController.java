/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientapplication;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.json.JsonObject;
import model.CommentModel;
import model.*;
import network.JsonConst;
import network.JsonUtil;
import network.RequestHandler;

/**
 * FXML Controller class
 *
 * @author esma
 */
public class TaskViewController implements Initializable {

    @FXML
    private Label titleLabel;
    @FXML
    private DatePicker assignDatePicker;
    @FXML
    private DatePicker deadlineDatePicker;
    @FXML
    private TextArea descriptionTextArea;
    @FXML
    private TextArea commentTextArea;
    @FXML
    private ListView<CommentModel> commentsListView;
    @FXML
    private ComboBox statusComboBox;
    @FXML
    private ComboBox assignToComboBox;
    @FXML
    private TextField titleTextField;

    private static final String TO_DO = "To Do";
    private static final String IN_PROGRESS = "In Progress";
    private static final String DONE = "Done";

    private ObservableList<String> statusList = FXCollections.observableArrayList(TO_DO, IN_PROGRESS, DONE);
    private ObservableList<String> collaboratorList = FXCollections.observableArrayList();
    private ObservableList<CommentModel> commentsList = FXCollections.observableArrayList();

    private List<UserModel> users = new ArrayList<>();
    private List<CommentModel> comments = new ArrayList<>();
    private int taskID;
    private boolean isEdit = false;
    private boolean isNew = true;
    private TaskModel selectedTask;
    @FXML
    private Button saveButton;
    @FXML
    private Button editButton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusComboBox.setItems(statusList);
        assignDatePicker.getEditor().setDisable(true);
        deadlineDatePicker.getEditor().setDisable(true);

    }

    public void setFromLastView(boolean isNew, TaskModel task) {
        this.isNew = isNew;
        this.taskID = task.getTask_id();
        this.selectedTask = task;
        if (this.isNew) {
            editButton.setDisable(true);
            titleLabel.setText("Add Task");
            resetFields();

        } else {
            editButton.setDisable(false);
            titleLabel.setText("Task Details");
            titleTextField.setText(selectedTask.getTitle());
            descriptionTextArea.setText(selectedTask.getDescription());
            assignDatePicker.setValue(selectedTask.getAssign_date().toLocalDateTime().toLocalDate());
            deadlineDatePicker.setValue(selectedTask.getDeadline().toLocalDateTime().toLocalDate());
            switch (selectedTask.getTask_status()) {
                case TaskModel.TASK_STATUS.TODO:
                    statusComboBox.setValue(TO_DO);
                    break;
                case TaskModel.TASK_STATUS.INPROGRESS:
                    statusComboBox.setValue(IN_PROGRESS);
                    break;
                case TaskModel.TASK_STATUS.DONE:
                    statusComboBox.setValue(DONE);
                    break;
            }

            assignToComboBox.setValue(selectedTask.getUser_name());
            saveButton.setDisable(true);
            disableOrEnableFields(true);
            getAllComments();

        }
        getAllCollaborators();

    }

    private void getAllCollaborators() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //request the list of collaborators
                JsonObject jsonObject = JsonUtil.fromId(JsonConst.TYPE_COLLABORATOR_LIST, selectedTask.getList_id());
                JsonObject response = new RequestHandler().makeRequest(jsonObject);
                users = JsonUtil.toUsersList(response);
                for (UserModel user : users) {
                    collaboratorList.add(user.getName());
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        assignToComboBox.setItems(collaboratorList);
                    }
                });
            }

        }).start();
    }

    private void getAllComments() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JsonObject commentsRequest = JsonUtil.fromTaskId(taskID);
                JsonObject commentsResponse = new RequestHandler().makeRequest(commentsRequest);
                comments = JsonUtil.fromJsonCommentsList(commentsResponse);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        //TODO: get collabors names from db
                        commentsList.setAll(comments);
                        commentsListView.setItems(commentsList);
                        commentsListView.setCellFactory(param -> new Cell());
                    }
                });
            }
        }).start();

    }

    @FXML
    private void saveButtonPressed(ActionEvent event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TaskModel task = new TaskModel();
                task.setTitle(titleTextField.getText());
                task.setDescription(descriptionTextArea.getText());
                String status = null;
                switch (statusComboBox.getValue().toString()) {
                    case TO_DO:
                        status = TaskModel.TASK_STATUS.TODO;
                        break;

                    case IN_PROGRESS:
                        status = TaskModel.TASK_STATUS.INPROGRESS;
                        break;

                    case DONE:
                        status = TaskModel.TASK_STATUS.DONE;
                        break;
                }

                task.setTask_status(status);
                task.setDeadline(Timestamp.valueOf(deadlineDatePicker.getValue().atStartOfDay()));
                //to get from List 
                task.setList_id(selectedTask.getList_id());
                int userIndex = assignToComboBox.getSelectionModel().getSelectedIndex();
                task.setUser_id(users.get(userIndex).getId());
                task.setAssign_date(Timestamp.valueOf(assignDatePicker.getValue().atStartOfDay()));
                task.setAssign_status(TaskModel.ASSIGN_STATUS.PENDING);
                task.setTask_id(taskID);
                JsonObject jsonObject = null;
                if (isEdit) {
                    jsonObject = JsonUtil.updateFromTask(task);
                    JsonObject response = new RequestHandler().makeRequest(jsonObject);
                } else {
                    jsonObject = JsonUtil.fromTask(task);
                    JsonObject response = new RequestHandler().makeRequest(jsonObject);
                    taskID = response.getInt(JsonConst.ID);
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        disableOrEnableFields(true);
                        editButton.setDisable(false);
                        saveButton.setDisable(true);
                    }
                });

            }
        }).start();

    }

    @FXML
    private void editButtonPressed(ActionEvent event) {
        disableOrEnableFields(false);
        editButton.setDisable(true);
        saveButton.setDisable(false);
        isEdit = true;
    }

    @FXML
    private void addCommentPressed(ActionEvent event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CommentModel comment = new CommentModel();
                comment.setTask_id(taskID);
                comment.setUser_id(1); // pending captian mazen
                comment.setComment_text(commentTextArea.getText());
                comment.setComment_date(new Timestamp(new Date().getTime()));
                JsonObject request = JsonUtil.fromComment(comment);
                System.out.println(request);
                new RequestHandler().makeRequest(request);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        commentTextArea.setText("");
                        getAllComments();
                    }
                });
            }
        }).start();

    }

    @FXML
    private void cancelButtonPressed(ActionEvent event) {
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("HomeView.fxml"));
//                        HomeController homeController = loader.getController();
//                        homeController.updateTasksLists(selectedTask.getList_id());

        ((Node) (event.getSource())).getScene().getWindow().hide();

        //((Stage)((Node) event.getSource()).getScene().getWindow()).close();
    }

    static class Cell extends ListCell<CommentModel> {

        VBox vbox = new VBox();
        HBox hbox = new HBox();
        Pane pane = new Pane();
        Label userName = new Label();
        Label date = new Label();
        TextArea commentsArea = new TextArea();

        @Override
        protected void updateItem(CommentModel item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {
                userName.setText(item.getUserName());
                commentsArea.setText(item.getComment_text());
                date.setText(item.getComment_date().toString());

                //setGraphic(image);
                setGraphic(vbox);
            }
        }

        public Cell() {
            super();
            hbox.getChildren().addAll(userName, pane, date);
            hbox.setHgrow(pane, Priority.ALWAYS);

            vbox.getChildren().addAll(hbox, commentsArea);
            vbox.setVgrow(pane, Priority.ALWAYS);
            commentsArea.setPrefHeight(50);
            commentsArea.setPrefWidth(50);
            commentsArea.setEditable(false);

        }

    }

    private void resetFields() {
        titleTextField.setText("");
        titleTextField.setDisable(false);
        assignToComboBox.getSelectionModel().clearSelection();
        assignToComboBox.setDisable(false);
        assignDatePicker.setDisable(false);
        assignDatePicker.setDisable(false);
        assignDatePicker.setValue(null);

        deadlineDatePicker.setValue(null);

        statusComboBox.getSelectionModel().clearSelection();
        statusComboBox.setDisable(false);;
        descriptionTextArea.setDisable(false);;
        descriptionTextArea.setText("");
    }

    private void disableOrEnableFields(boolean state) {
        titleTextField.setDisable(state);
        assignToComboBox.setDisable(state);
        assignDatePicker.setDisable(state);
        deadlineDatePicker.setDisable(state);
        assignDatePicker.setDisable(state);
        deadlineDatePicker.setDisable(state);
        statusComboBox.setDisable(state);
        descriptionTextArea.setDisable(state);

    }

}
