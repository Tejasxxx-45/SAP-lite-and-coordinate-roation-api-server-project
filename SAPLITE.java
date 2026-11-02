import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * SAP-Lite Inventory Management System
 * Beginner-friendly Java Swing project.
 *
 * Local storage:
 *   data/users.csv
 *   data/inventory.csv
 *   data/logs.csv
 */
public class SAPLITE {

    static final String DATA_DIR = "data";
    static final String USERS_FILE = DATA_DIR + File.separator + "users.csv";
    static final String INVENTORY_FILE = DATA_DIR + File.separator + "inventory.csv";
    static final String LOGS_FILE = DATA_DIR + File.separator + "logs.csv";

    static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static final List<Item> inventory = new ArrayList<>();

    public static void main(String[] args) {
        createDataFiles();
        loadInventory();

        SwingUtilities.invokeLater(() -> showLogin());
    }

    // -------------------- DATA MODELS --------------------

    static class Item {
        String reference;
        String name;
        String category;
        int quantity;
        String location;

        Item(String reference, String name, String category,
             int quantity, String location) {
            this.reference = reference;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.location = location;
        }
    }

    // -------------------- FILE HANDLING --------------------

    static void createDataFiles() {
        try {
            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File users = new File(USERS_FILE);
            if (!users.exists()) {
                try (PrintWriter out = new PrintWriter(new FileWriter(users))) {
                    out.println("username,password_hash");
                    out.println("admin," + hashPassword("admin123"));
                }
            }

            File inv = new File(INVENTORY_FILE);
            if (!inv.exists()) {
                try (PrintWriter out = new PrintWriter(new FileWriter(inv))) {
                    out.println("reference,name,category,quantity,location");
                }
            }

            File logs = new File(LOGS_FILE);
            if (!logs.exists()) {
                try (PrintWriter out = new PrintWriter(new FileWriter(logs))) {
                    out.println("date_time,type,reference,item,quantity,user");
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not create data files:\n" + e.getMessage(),
                    "File Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    static void loadInventory() {
        inventory.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(INVENTORY_FILE))) {
            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {
                String[] p = parseCsv(line);

                if (p.length >= 5) {
                    try {
                        int qty = Integer.parseInt(p[3]);
                        inventory.add(new Item(p[0], p[1], p[2], qty, p[4]));
                    } catch (NumberFormatException ignored) {
                        // Ignore damaged inventory rows.
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read inventory.csv:\n" + e.getMessage(),
                    "File Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    static void saveInventory() {
        try (PrintWriter out = new PrintWriter(new FileWriter(INVENTORY_FILE))) {
            out.println("reference,name,category,quantity,location");

            for (Item item : inventory) {
                out.println(
                        csv(item.reference) + "," +
                                csv(item.name) + "," +
                                csv(item.category) + "," +
                                item.quantity + "," +
                                csv(item.location)
                );
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not save inventory:\n" + e.getMessage(),
                    "File Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    static void writeLog(String type, Item item, int quantity, String user) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOGS_FILE, true))) {
            out.println(
                    csv(LocalDateTime.now().format(TIME_FORMAT)) + "," +
                            csv(type) + "," +
                            csv(item.reference) + "," +
                            csv(item.name) + "," +
                            quantity + "," +
                            csv(user)
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not write log:\n" + e.getMessage(),
                    "File Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    static String csv(String value) {
        if (value == null) return "";
        value = value.replace("\"", "\"\"");
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    static String[] parseCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    // -------------------- LOGIN --------------------

    static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean checkLogin(String username, String password) {
        String enteredHash = hashPassword(password);

        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            br.readLine(); // header

            String line;

            while ((line = br.readLine()) != null) {
                String[] p = parseCsv(line);

                if (p.length >= 2) {
                    String savedUsername = p[0].trim();
                    String savedHash = p[1].trim();

                    if (savedUsername.equals(username.trim())
                            && savedHash.equals(enteredHash)) {
                        return true;
                    }
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read users.csv:\n" + e.getMessage(),
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        return false;
    }

    static void showLogin() {
        JFrame frame = new JFrame("SAP-Lite Inventory - Login");
        frame.setSize(430, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel title = new JLabel("Inventory Management System", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel subtitle = new JLabel("Local SAP-style stock management", SwingConstants.CENTER);

        JPanel fields = new JPanel(new GridLayout(3, 2, 8, 8));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        fields.add(new JLabel("Username:"));
        fields.add(username);
        fields.add(new JLabel("Password:"));
        fields.add(password);

        JButton loginButton = new JButton("Login");
        fields.add(new JLabel());
        fields.add(loginButton);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(title);
        top.add(subtitle);

        main.add(top, BorderLayout.NORTH);
        main.add(fields, BorderLayout.CENTER);

        JLabel info = new JLabel("Default: admin / admin123", SwingConstants.CENTER);
        main.add(info, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> {
            String user = username.getText();
            String pass = new String(password.getPassword());

            if (checkLogin(user, pass)) {
                frame.dispose();
                showDashboard(user);
            } else {
                JOptionPane.showMessageDialog(
                        frame,
                        "Invalid username or password.",
                        "Login Failed",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        frame.setContentPane(main);
        frame.setVisible(true);
    }

    // -------------------- DASHBOARD --------------------

    static void showDashboard(String loggedInUser) {
        JFrame frame = new JFrame("SAP-Lite Inventory Management System");
        frame.setSize(1000, 650);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Inventory Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JLabel userLabel = new JLabel("Logged in: " + loggedInUser);

        header.add(title, BorderLayout.WEST);
        header.add(userLabel, BorderLayout.EAST);

        main.add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Reference", "Item Name", "Category", "Quantity", "Location"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(25);

        JScrollPane scroll = new JScrollPane(table);
        main.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(2, 4, 8, 8));

        JButton addButton = new JButton("Add Item");
        JButton stockInButton = new JButton("Stock IN");
        JButton stockOutButton = new JButton("Stock OUT");
        JButton refreshButton = new JButton("Refresh");

        JButton logsButton = new JButton("View Logs");
        JButton logoutButton = new JButton("Logout");

        JLabel totalItems = new JLabel("Items: 0");
        JLabel totalStock = new JLabel("Total Stock: 0");

        bottom.add(addButton);
        bottom.add(stockInButton);
        bottom.add(stockOutButton);
        bottom.add(refreshButton);
        bottom.add(logsButton);
        bottom.add(logoutButton);
        bottom.add(totalItems);
        bottom.add(totalStock);

        main.add(bottom, BorderLayout.SOUTH);

        Runnable refreshTable = () -> {
            model.setRowCount(0);

            int stock = 0;

            for (Item item : inventory) {
                model.addRow(new Object[]{
                        item.reference,
                        item.name,
                        item.category,
                        item.quantity,
                        item.location
                });

                stock += item.quantity;
            }

            totalItems.setText("Items: " + inventory.size());
            totalStock.setText("Total Stock: " + stock);
        };

        addButton.addActionListener(e -> showAddItemDialog(frame, loggedInUser, refreshTable));

        stockInButton.addActionListener(e ->
                showStockDialog(frame, loggedInUser, true, table, refreshTable)
        );

        stockOutButton.addActionListener(e ->
                showStockDialog(frame, loggedInUser, false, table, refreshTable)
        );

        refreshButton.addActionListener(e -> {
            loadInventory();
            refreshTable.run();
        });

        logsButton.addActionListener(e -> showLogs(frame));

        logoutButton.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        refreshTable.run();

        frame.setContentPane(main);
        frame.setVisible(true);
    }

    // -------------------- ADD ITEM --------------------

    static void showAddItemDialog(JFrame parent, String user, Runnable refresh) {
        JTextField ref = new JTextField();
        JTextField name = new JTextField();
        JTextField category = new JTextField();
        JTextField quantity = new JTextField("0");
        JTextField location = new JTextField();

        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));

        panel.add(new JLabel("Reference / SKU:"));
        panel.add(ref);

        panel.add(new JLabel("Item Name:"));
        panel.add(name);

        panel.add(new JLabel("Category:"));
        panel.add(category);

        panel.add(new JLabel("Opening Quantity:"));
        panel.add(quantity);

        panel.add(new JLabel("Storage Location:"));
        panel.add(location);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Add Inventory Item",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String reference = ref.getText().trim();
            String itemName = name.getText().trim();
            String itemCategory = category.getText().trim();
            int qty = Integer.parseInt(quantity.getText().trim());
            String itemLocation = location.getText().trim();

            if (reference.isEmpty() || itemName.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Reference and item name are required.");
                return;
            }

            if (qty < 0) {
                JOptionPane.showMessageDialog(parent, "Quantity cannot be negative.");
                return;
            }

            for (Item item : inventory) {
                if (item.reference.equalsIgnoreCase(reference)) {
                    JOptionPane.showMessageDialog(
                            parent,
                            "This reference already exists."
                    );
                    return;
                }
            }

            Item item = new Item(
                    reference,
                    itemName,
                    itemCategory,
                    qty,
                    itemLocation
            );

            inventory.add(item);
            saveInventory();

            if (qty > 0) {
                writeLog("OPENING STOCK", item, qty, user);
            }

            refresh.run();

            JOptionPane.showMessageDialog(parent, "Item added successfully.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Quantity must be a whole number."
            );
        }
    }

    // -------------------- STOCK IN / OUT --------------------

    static void showStockDialog(
            JFrame parent,
            String user,
            boolean stockIn,
            JTable table,
            Runnable refresh
    ) {
        if (inventory.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No inventory items exist yet.");
            return;
        }

        String[] references = new String[inventory.size()];

        for (int i = 0; i < inventory.size(); i++) {
            references[i] = inventory.get(i).reference
                    + " - "
                    + inventory.get(i).name;
        }

        JComboBox<String> itemBox = new JComboBox<>(references);
        JTextField qtyField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Inventory Item:"));
        panel.add(itemBox);
        panel.add(new JLabel("Quantity:"));
        panel.add(qtyField);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                stockIn ? "Stock IN" : "Stock OUT",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int qty = Integer.parseInt(qtyField.getText().trim());

            if (qty <= 0) {
                JOptionPane.showMessageDialog(parent, "Quantity must be greater than zero.");
                return;
            }

            int index = itemBox.getSelectedIndex();
            Item item = inventory.get(index);

            if (!stockIn && qty > item.quantity) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Stock OUT cannot be greater than current stock (" +
                                item.quantity + ")."
                );
                return;
            }

            if (stockIn) {
                item.quantity += qty;
                writeLog("STOCK IN", item, qty, user);
            } else {
                item.quantity -= qty;
                writeLog("STOCK OUT", item, qty, user);
            }

            saveInventory();
            refresh.run();

            JOptionPane.showMessageDialog(
                    parent,
                    (stockIn ? "Stock IN" : "Stock OUT") +
                            " recorded successfully."
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Enter a valid whole number."
            );
        }
    }

    // -------------------- LOG VIEW --------------------

    static void showLogs(JFrame parent) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Date & Time", "Type", "Reference", "Item", "Quantity", "User"},
                0
        );

        try (BufferedReader br = new BufferedReader(new FileReader(LOGS_FILE))) {
            br.readLine(); // header

            String line;

            while ((line = br.readLine()) != null) {
                String[] p = parseCsv(line);

                if (p.length >= 6) {
                    model.addRow(new Object[]{
                            p[0], p[1], p[2], p[3], p[4], p[5]
                    });
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not read logs.csv:\n" + e.getMessage()
            );
            return;
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(850, 400));

        JOptionPane.showMessageDialog(
                parent,
                scroll,
                "Stock Movement Logs",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
