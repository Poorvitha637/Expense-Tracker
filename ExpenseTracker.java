import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ExpenseTracker extends JFrame {

    // ─── Data ────────────────────────────────────────────────────────────────
    private final ArrayList<Expense> expenses = new ArrayList<>();
    private DefaultTableModel tableModel;
    private double monthlyIncome = 0;

    // ─── Professional Palette (Slate Blue / Teal / White) ────────────────────
    private static final Color BG           = new Color(248, 249, 252);  // off-white bg
    private static final Color SURFACE      = new Color(255, 255, 255);  // pure white cards
    private static final Color SURFACE2     = new Color(241, 244, 248);  // subtle fill
    private static final Color PRIMARY      = new Color(37,  99,  235);  // professional blue
    private static final Color PRIMARY_DARK = new Color(29,  78,  216);  // hover blue
    private static final Color PRIMARY_SOFT = new Color(219, 234, 254);  // light blue fill
    private static final Color TEAL         = new Color(13,  148, 136);  // teal accent
    private static final Color TEAL_SOFT    = new Color(204, 251, 241);  // light teal
    private static final Color AMBER        = new Color(217, 119,   6);  // amber warning
    private static final Color AMBER_SOFT   = new Color(254, 243, 199);  // amber fill
    private static final Color SUCCESS      = new Color(22,  163,  74);  // green success
    private static final Color SUCCESS_SOFT = new Color(220, 252, 231);  // green fill
    private static final Color DANGER       = new Color(220,  38,  38);  // red error
    private static final Color DANGER_SOFT  = new Color(254, 226, 226);  // red fill
    private static final Color TEXT1        = new Color(15,  23,  42);   // slate-900 primary
    private static final Color TEXT2        = new Color(71,  85,  105);  // slate-500 secondary
    private static final Color TEXT3        = new Color(148, 163, 184);  // slate-400 muted
    private static final Color BORDER       = new Color(226, 232, 240);  // slate-200

    // ─── Fonts ───────────────────────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Segoe UI",   Font.BOLD,  20);
    private static final Font F_HEADING= new Font("Segoe UI",   Font.BOLD,  13);
    private static final Font F_LABEL  = new Font("Segoe UI",   Font.PLAIN, 12);
    private static final Font F_BODY   = new Font("Segoe UI",   Font.PLAIN, 13);
    private static final Font F_MONO   = new Font("Consolas",   Font.BOLD,  14);
    private static final Font F_SMALL  = new Font("Segoe UI",   Font.PLAIN, 11);
    private static final Font F_STAT   = new Font("Segoe UI",   Font.BOLD,  22);
    private static final Font F_SUB    = new Font("Segoe UI",   Font.PLAIN, 12);

    // ─── Category config ─────────────────────────────────────────────────────
    private static final String[] CATS  = {"Food", "Travel", "Shopping", "Bills", "Others"};
    private static final Color[]  CCOLS = {
        new Color(37,  99,  235),   // Food     – blue
        new Color(13,  148, 136),   // Travel   – teal
        new Color(124,  58, 237),   // Shopping – violet
        new Color(217, 119,   6),   // Bills    – amber
        new Color(220,  38,  38)    // Others   – red
    };

    // ─── Widgets ─────────────────────────────────────────────────────────────
    private JTextField        amountField, dateField;
    private JComboBox<String> categoryBox;
    private JLabel            totalLbl, remainLbl, savingsLbl, alertLbl, incomeLbl;
    private JProgressBar      budgetBar;
    private final Map<String, JProgressBar> catBars    = new LinkedHashMap<>();
    private final Map<String, JLabel>       catAmtLbls = new LinkedHashMap<>();

    // ─── Charts panel references ──────────────────────────────────────────────
    private MonthlyChart monthlyBar;

    // =========================================================================
    public ExpenseTracker() {
        setTitle("Expense Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 960);
        setMinimumSize(new Dimension(620, 720));
        setLocationRelativeTo(null);
        setBackground(BG);

        askMonthlyIncome();

        JPanel root = buildRoot();
        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll);

        loadFromFile();
        refreshTable();
        updateInsights();
    }

    // =========================================================================
    private JPanel buildRoot() {
        JPanel root = new JPanel();
        root.setBackground(BG);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(28, 36, 36, 36));

        root.add(buildHeader());
        root.add(gap(20));
        root.add(buildSummaryRow());
        root.add(gap(16));
        root.add(buildBudgetCard());
        root.add(gap(16));
        root.add(buildChartsRow());
        root.add(gap(16));
        root.add(buildInputCard());
        root.add(gap(16));
        root.add(buildCategoryCard());
        root.add(gap(16));
        root.add(buildTableCard());
        root.add(gap(16));
        root.add(buildFooterButtons());
        return root;
    }

    // ─── Header ───────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Expense Tracker");
        title.setFont(F_TITLE);
        title.setForeground(TEXT1);
        JLabel sub = new JLabel("Monitor your spending and savings");
        sub.setFont(F_SUB);
        sub.setForeground(TEXT2);
        left.add(title);
        left.add(gap(3));
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        incomeLbl = new JLabel(monthlyIncome > 0
                ? "Income: \u20B9" + fmt0(monthlyIncome) + "/mo" : "No income set");
        incomeLbl.setFont(F_LABEL);
        incomeLbl.setForeground(TEXT2);
        JButton chg = linkButton("Change");
        chg.addActionListener(e -> {
            askMonthlyIncome();
            incomeLbl.setText(monthlyIncome > 0
                    ? "Income: \u20B9" + fmt0(monthlyIncome) + "/mo" : "No income set");
            updateInsights();
        });
        right.add(incomeLbl);
        right.add(Box.createHorizontalStrut(8));
        right.add(chg);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        return p;
    }

    // ─── Summary row (3 stat cards) ───────────────────────────────────────────
    private JPanel buildSummaryRow() {
        JPanel p = new JPanel(new GridLayout(1, 3, 12, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);

        p.add(statCard("Total Spent", "\u20B90.00", PRIMARY, PRIMARY_SOFT, true));
        p.add(statCard("Remaining", "Set income", SUCCESS, SUCCESS_SOFT, false));
        p.add(statCard("Savings Rate", "\u2014", TEAL, TEAL_SOFT, false));
        return p;
    }

    private JPanel statCard(String label, String value, Color col, Color bg, boolean isTotalCard) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        // Colored top accent line via custom painting
        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(col);
                g.fillRect(0, 0, getWidth(), 3);
            }
        };
        accent.setPreferredSize(new Dimension(0, 3));
        accent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        accent.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(F_LABEL);
        lbl.setForeground(TEXT2);

        JLabel val = new JLabel(value);
        val.setFont(F_STAT);
        val.setForeground(col);

        if (isTotalCard) totalLbl = val;
        else if (label.equals("Remaining")) remainLbl = val;
        else savingsLbl = val;

        card.add(lbl);
        card.add(gap(8));
        card.add(val);
        return card;
    }

    // ─── Budget card ──────────────────────────────────────────────────────────
    private JPanel buildBudgetCard() {
        JPanel card = surfaceCard();

        JLabel hdr = sectionLabel("Budget Overview");
        card.add(hdr);
        card.add(gap(12));

        budgetBar = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE2);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                if (getValue() > 0) {
                    int w = (int)(getWidth() * getValue() / 100.0);
                    g2.setColor(getForeground());
                    g2.fillRoundRect(0, 0, Math.max(w, getHeight()), getHeight(), getHeight(), getHeight());
                }
            }
        };
        budgetBar.setPreferredSize(new Dimension(0, 14));
        budgetBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        budgetBar.setBorderPainted(false);
        budgetBar.setStringPainted(false);
        budgetBar.setForeground(SUCCESS);
        budgetBar.setAlignmentX(LEFT_ALIGNMENT);
        card.add(budgetBar);
        card.add(gap(10));

        alertLbl = new JLabel(" ");
        alertLbl.setFont(F_SMALL);
        alertLbl.setForeground(TEXT2);
        card.add(alertLbl);
        return card;
    }

    // ─── Monthly tracker chart ────────────────────────────────────────────────
    private JPanel buildChartsRow() {
        JPanel card = surfaceCard();

        // Header row: title + year navigator
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        header.add(sectionLabel("Monthly Tracker"), BorderLayout.WEST);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        nav.setOpaque(false);
        JButton prevYear = linkButton("< " + (LocalDate.now().getYear() - 1));
        JButton nextYear = linkButton((LocalDate.now().getYear() + 1) + " >");
        JLabel  yearLbl  = new JLabel(String.valueOf(LocalDate.now().getYear()));
        yearLbl.setFont(F_HEADING);
        yearLbl.setForeground(TEXT1);

        prevYear.addActionListener(e -> {
            monthlyBar.shiftYear(-1);
            yearLbl.setText(String.valueOf(monthlyBar.getYear()));
            prevYear.setText("< " + (monthlyBar.getYear() - 1));
            nextYear.setText((monthlyBar.getYear() + 1) + " >");
        });
        nextYear.addActionListener(e -> {
            monthlyBar.shiftYear(+1);
            yearLbl.setText(String.valueOf(monthlyBar.getYear()));
            prevYear.setText("< " + (monthlyBar.getYear() - 1));
            nextYear.setText((monthlyBar.getYear() + 1) + " >");
        });

        nav.add(prevYear);
        nav.add(Box.createHorizontalStrut(4));
        nav.add(yearLbl);
        nav.add(Box.createHorizontalStrut(4));
        nav.add(nextYear);
        header.add(nav, BorderLayout.EAST);
        card.add(header);
        card.add(gap(6));

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(LEFT_ALIGNMENT);
        legend.add(legendDot(PRIMARY,  "Expenses"));
        legend.add(legendDot(SUCCESS,  "Savings"));
        legend.add(legendDot(TEAL,     "Income line"));
        card.add(legend);
        card.add(gap(10));

        monthlyBar = new MonthlyChart();
        monthlyBar.setPreferredSize(new Dimension(0, 220));
        monthlyBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        monthlyBar.setAlignmentX(LEFT_ALIGNMENT);
        card.add(monthlyBar);

        return card;
    }

    private JPanel legendDot(Color col, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col);
                g2.fillRoundRect(0, 2, 12, 8, 3, 3);
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(12, 12));
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_SMALL);
        lbl.setForeground(TEXT2);
        p.add(dot);
        p.add(lbl);
        return p;
    }

    // ─── Input card ───────────────────────────────────────────────────────────
    private JPanel buildInputCard() {
        JPanel card = surfaceCard();
        card.add(sectionLabel("Add Expense"));
        card.add(gap(14));

        JPanel grid = new JPanel(new GridLayout(1, 3, 12, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel amtP = fieldGroup("Amount (\u20B9)");
        amountField = styledField("0.00");
        amtP.add(amountField);

        JPanel catP = fieldGroup("Category");
        categoryBox = new JComboBox<>(CATS);
        styleCombo(categoryBox);
        catP.add(categoryBox);

        JPanel dateP = fieldGroup("Date (DD-MM-YYYY)");
        dateField = styledField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        dateP.add(dateField);

        grid.add(amtP);
        grid.add(catP);
        grid.add(dateP);
        card.add(grid);
        card.add(gap(14));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(LEFT_ALIGNMENT);

        JButton addBtn = primaryButton("+ Add Expense");
        addBtn.addActionListener(e -> addExpense());
        JButton delBtn = ghostButtonDark("Delete Selected");
        delBtn.addActionListener(e -> deleteSelected());
        btns.add(addBtn);
        btns.add(Box.createHorizontalStrut(10));
        btns.add(delBtn);
        card.add(btns);

        return card;
    }

    private JPanel fieldGroup(String label) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_LABEL);
        lbl.setForeground(TEXT2);
        p.add(lbl);
        p.add(gap(5));
        return p;
    }

    // ─── Category breakdown card ──────────────────────────────────────────────
    private JPanel buildCategoryCard() {
        JPanel card = surfaceCard();
        card.add(sectionLabel("Category Breakdown"));
        card.add(gap(14));

        for (int i = 0; i < CATS.length; i++) {
            String cat = CATS[i];
            Color  col = CCOLS[i];

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            // Color dot + name
            JPanel catLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            catLeft.setOpaque(false);
            catLeft.setPreferredSize(new Dimension(110, 24));

            DotLabel dot = new DotLabel(col);
            JLabel catLbl = new JLabel("  " + cat);
            catLbl.setFont(F_BODY);
            catLbl.setForeground(TEXT1);
            catLeft.add(dot);
            catLeft.add(catLbl);

            JProgressBar bar = new JProgressBar(0, 100) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(SURFACE2);
                    int h = getHeight() - 6;
                    g2.fillRoundRect(0, 3, getWidth(), h, h, h);
                    if (getValue() > 0) {
                        int w = (int)(getWidth() * getValue() / 100.0);
                        // Soft fill version of category color
                        Color soft = new Color(col.getRed(), col.getGreen(), col.getBlue(), 160);
                        g2.setColor(soft);
                        g2.fillRoundRect(0, 3, Math.max(w, h), h, h, h);
                    }
                }
            };
            bar.setStringPainted(false);
            bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(0, 24));

            JLabel amt = new JLabel("\u20B90", SwingConstants.RIGHT);
            amt.setFont(new Font("Consolas", Font.BOLD, 12));
            amt.setForeground(TEXT2);
            amt.setPreferredSize(new Dimension(80, 24));

            catBars.put(cat, bar);
            catAmtLbls.put(cat, amt);

            row.add(catLeft, BorderLayout.WEST);
            row.add(bar,     BorderLayout.CENTER);
            row.add(amt,     BorderLayout.EAST);
            card.add(row);
            card.add(gap(6));
        }
        return card;
    }

    // ─── Table card ───────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = surfaceCard();
        card.add(sectionLabel("Expense Log"));
        card.add(gap(12));

        String[] cols = {"#", "Amount", "Category", "Description", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 210));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(SURFACE);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        card.add(scroll);
        return card;
    }

    // ─── Footer ───────────────────────────────────────────────────────────────
    private JPanel buildFooterButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);

        JButton save = secondaryButton("Save to File");
        
        save.addActionListener(e -> saveToFile());
        
        p.add(save);
        p.add(Box.createHorizontalStrut(10));
       
        return p;
    }

    // ─── Income dialog ────────────────────────────────────────────────────────
    private void askMonthlyIncome() {
        JTextField inp = styledField("e.g. 25000");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(12, 4, 4, 4));
        JLabel l = new JLabel("Monthly income (₹):");
        l.setFont(F_BODY);
        l.setForeground(TEXT1);
        panel.add(l);
        panel.add(gap(8));
        panel.add(inp);

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, panel,
                    "Set Monthly Income", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) { monthlyIncome = 0; break; }
            try {
                double v = Double.parseDouble(inp.getText().trim());
                if (v < 0) throw new NumberFormatException();
                monthlyIncome = v; break;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Enter a valid positive number.",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ─── Business logic ───────────────────────────────────────────────────────
    private void addExpense() {
        String amtTxt  = amountField.getText().trim();
        String cat     = (String) categoryBox.getSelectedItem();
        String dateTxt = dateField.getText().trim();

        if (amtTxt.isEmpty()) { alert("Please enter an amount."); return; }
        double amount;
        try {
            amount = Double.parseDouble(amtTxt);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) { alert("Enter a valid positive number."); return; }

        try {
            DateTimeFormatter.ofPattern("dd-MM-yyyy").parse(dateTxt);
        } catch (DateTimeParseException ex) { alert("Date must be DD-MM-YYYY."); return; }

        String reason = "";
        if ("Others".equals(cat)) {
            reason = askOthersReason();
            if (reason == null) return;
            if (reason.trim().isEmpty()) reason = "Miscellaneous";
        }

        expenses.add(new Expense(amount, cat, dateTxt, reason));
        amountField.setText("");
        refreshTable();
        updateInsights();
    }

    private String askOthersReason() {
        JTextField inp = styledField("e.g. Gift, Medical...");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(12, 4, 4, 4));
        JLabel l = new JLabel("Description for 'Others':");
        l.setFont(F_BODY); l.setForeground(TEXT1);
        panel.add(l); panel.add(gap(8)); panel.add(inp);
        int r = JOptionPane.showConfirmDialog(this, panel,
                "Add Description", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return null;
        return inp.getText().trim();
    }

    private void deleteSelected() {
        JTable tbl = findTable(this);
        if (tbl == null) return;
        int row = tbl.getSelectedRow();
        if (row < 0) { alert("Select a row to delete."); return; }
        expenses.remove(row);
        refreshTable();
        updateInsights();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < expenses.size(); i++) {
            Expense e = expenses.get(i);
            tableModel.addRow(new Object[]{
                i + 1,
                String.format("%.2f", e.amount),
                e.category,
                (e.reason == null || e.reason.isEmpty()) ? "\u2014" : e.reason,
                e.date
            });
        }
    }

    private void updateInsights() {
        double total = 0;
        Map<String, Double> cm = new LinkedHashMap<>();
        for (String c : CATS) cm.put(c, 0.0);
        for (Expense e : expenses) { total += e.amount; cm.merge(e.category, e.amount, Double::sum); }

        totalLbl.setText(String.format("\u20B9%.2f", total));

        // Budget / remaining
        if (monthlyIncome > 0) {
            double rem = monthlyIncome - total;
            double pct = Math.min(total / monthlyIncome * 100, 100);
            budgetBar.setValue((int) pct);
            budgetBar.setForeground(pct < 60 ? SUCCESS : pct < 85 ? AMBER : DANGER);

            remainLbl.setText(rem >= 0
                    ? String.format("\u20B9%.2f", rem)
                    : String.format("-\u20B9%.2f", Math.abs(rem)));
            remainLbl.setForeground(rem >= 0 ? SUCCESS : DANGER);

            double savPct = rem >= 0 ? (rem / monthlyIncome * 100) : 0;
            savingsLbl.setText(String.format("%.1f%%", savPct));
            savingsLbl.setForeground(TEAL);
        } else {
            budgetBar.setValue(0);
            remainLbl.setText("Set income");
            remainLbl.setForeground(TEXT3);
            savingsLbl.setText("\u2014");
            savingsLbl.setForeground(TEXT3);
        }

        // Alert
        double foodPct = total > 0 ? cm.getOrDefault("Food", 0.0) / total * 100 : 0;
        boolean over = monthlyIncome > 0 && total > monthlyIncome;
        boolean near = monthlyIncome > 0 && total > monthlyIncome * 0.85;

        if (over)       setAlert("\u26A0  Over monthly budget!", DANGER);
        else if (near)  setAlert("\u26A0  Over 85% of budget used", AMBER);
        else if (foodPct > 50 && total > 5000)
                        setAlert("\u26A0  Food exceeds 50% of total spending", AMBER);
        else if (total > 0) setAlert("\u2713  Spending is within budget", SUCCESS);
        else            setAlert("No expenses recorded yet", TEXT3);

        // Category bars
        for (String c : CATS) {
            double v = cm.getOrDefault(c, 0.0);
            catBars.get(c).setValue(total > 0 ? (int)(v / total * 100) : 0);
            catAmtLbls.get(c).setText(String.format("\u20B9%.0f", v));
        }

        // Update monthly chart
        if (monthlyBar != null) monthlyBar.updateData(expenses, monthlyIncome);
    }

    private void setAlert(String msg, Color col) {
        alertLbl.setText(msg);
        alertLbl.setForeground(col);
    }

    // ─── File I/O ─────────────────────────────────────────────────────────────
    private static final String FILE = "expenses.csv";

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("INCOME:" + monthlyIncome);
            for (Expense e : expenses)
                pw.printf("%.2f|%s|%s|%s%n", e.amount, e.category, e.date,
                        e.reason == null ? "" : e.reason.replace("|", "-"));
            JOptionPane.showMessageDialog(this,
                    "Saved " + expenses.size() + " record(s) to " + FILE, "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { alert("Save failed: " + ex.getMessage()); }
    }

    private void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return;
        expenses.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("INCOME:")) {
                    try { monthlyIncome = Double.parseDouble(line.substring(7)); }
                    catch (NumberFormatException ignored) {}
                    continue;
                }
                String[] p = line.split("\\|", 4);
                if (p.length < 3) continue;
                try {
                    expenses.add(new Expense(Double.parseDouble(p[0].trim()),
                            p[1].trim(), p[2].trim(), p.length == 4 ? p[3].trim() : ""));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ex) { alert("Load failed: " + ex.getMessage()); }
        if (incomeLbl != null)
            incomeLbl.setText(monthlyIncome > 0
                    ? "Income: \u20B9" + fmt0(monthlyIncome) + "/mo" : "No income set");
    }

    // =========================================================================
    //  Custom Chart Components
    // =========================================================================

    /**
     * Monthly Tracker — stacked bars (expenses + savings) for each month of the
     * selected year, with a dashed horizontal income line for reference.
     * Year can be navigated with shiftYear(±1).
     */
    class MonthlyChart extends JPanel {
        private static final String[] MONTH_LABELS =
            {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        private ArrayList<Expense> data  = new ArrayList<>();
        private double             income = 0;
        private int                year   = LocalDate.now().getYear();

        // Tooltip state
        private int    hoverMonth = -1;
        private String tooltip    = "";

        MonthlyChart() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    updateHover(e.getX(), e.getY());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hoverMonth = -1; tooltip = ""; repaint();
                }
            });
        }

        int  getYear() { return year; }
        void shiftYear(int delta) { year += delta; repaint(); }

        void updateData(ArrayList<Expense> exp, double inc) {
            this.data   = exp;
            this.income = inc;
            repaint();
        }

        private void updateHover(int mx, int my) {
            int w = getWidth(), h = getHeight();
            int padL = 52, padB = 32, padT = 16, padR = 12;
            int chartW = w - padL - padR;
            int groupW = chartW / 12;
            int barW   = Math.max(6, (int)(groupW * 0.6));

            double[] spent = monthlySpent();
            double   max   = computeMax(spent);

            for (int m = 0; m < 12; m++) {
                int bx = padL + m * groupW + (groupW - barW) / 2;
                int spentH = max > 0 ? (int)(spent[m] / max * (h - padT - padB)) : 0;
                int savH   = (income > 0 && income > spent[m])
                        ? (int)((income - spent[m]) / max * (h - padT - padB)) : 0;
                int totalBarH = spentH + savH;
                int by = padT + (h - padT - padB) - totalBarH;

                if (mx >= bx && mx <= bx + barW && my >= by && my <= padT + (h - padT - padB)) {
                    if (hoverMonth != m) {
                        hoverMonth = m;
                        double sav = income > 0 ? Math.max(0, income - spent[m]) : 0;
                        tooltip = MONTH_LABELS[m] + " " + year
                                + "  |  Spent: \u20B9" + fmt0(spent[m])
                                + "  Saved: \u20B9" + fmt0(sav);
                        repaint();
                    }
                    return;
                }
            }
            if (hoverMonth != -1) { hoverMonth = -1; tooltip = ""; repaint(); }
        }

        private double[] monthlySpent() {
            double[] totals = new double[12];
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            for (Expense e : data) {
                try {
                    LocalDate d = LocalDate.parse(e.date, fmt);
                    if (d.getYear() == year) totals[d.getMonthValue() - 1] += e.amount;
                } catch (Exception ignored) {}
            }
            return totals;
        }

        private double computeMax(double[] spent) {
            double max = income;
            for (double v : spent) max = Math.max(max, v);
            return max <= 0 ? 10000 : max * 1.15;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 52, padB = 32, padT = 16, padR = 12;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            double[] spent = monthlySpent();
            double   max   = computeMax(spent);

            // ── Grid lines ──────────────────────────────────────────────────
            int gridLines = 4;
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            for (int i = 0; i <= gridLines; i++) {
                int gy = padT + (int)(chartH * (1.0 - (double)i / gridLines));
                g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[]{3, 3}, 0));
                g2.setColor(BORDER);
                g2.drawLine(padL, gy, padL + chartW, gy);
                g2.setColor(TEXT3);
                String lbl = "\u20B9" + fmt0(max * i / gridLines);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, padL - fm.stringWidth(lbl) - 4, gy + 4);
            }

            // ── Bars ────────────────────────────────────────────────────────
            int groupW = chartW / 12;
            int barW   = Math.max(6, (int)(groupW * 0.62));
            int baseY  = padT + chartH;
            int radius = 4;

            for (int m = 0; m < 12; m++) {
                int bx = padL + m * groupW + (groupW - barW) / 2;
                int spentH = (int)(spent[m] / max * chartH);
                int savH   = (income > 0 && income > spent[m])
                        ? (int)((income - spent[m]) / max * chartH) : 0;

                boolean hover = (m == hoverMonth);

                // Savings segment (top)
                if (savH > 0) {
                    Color sc = hover ? SUCCESS.brighter() : SUCCESS;
                    g2.setColor(sc);
                    g2.setStroke(new BasicStroke(1));
                    // Rounded top
                    g2.fillRoundRect(bx, baseY - spentH - savH, barW, savH + radius, radius, radius);
                    // Flat bottom of savings (overlap with spent top)
                    g2.fillRect(bx, baseY - spentH, barW, radius);
                }

                // Expense segment (bottom)
                if (spentH > 0) {
                    Color ec = hover ? PRIMARY.brighter() : PRIMARY;
                    g2.setColor(ec);
                    if (savH > 0) {
                        // Flat top (savings sits above)
                        g2.fillRect(bx, baseY - spentH, barW, spentH);
                    } else {
                        // Rounded top when no savings
                        g2.fillRoundRect(bx, baseY - spentH, barW, spentH + radius, radius, radius);
                        g2.fillRect(bx, baseY - spentH + radius, barW, spentH);
                    }
                }

                // Empty bar placeholder
                if (spentH == 0 && savH == 0) {
                    g2.setColor(SURFACE2);
                    g2.fillRoundRect(bx, baseY - 4, barW, 4, 2, 2);
                }

                // Month label
                g2.setColor(hover ? PRIMARY : TEXT3);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(MONTH_LABELS[m],
                        bx + barW / 2 - fm.stringWidth(MONTH_LABELS[m]) / 2,
                        baseY + 14);
            }

            // ── Income dashed line ───────────────────────────────────────────
            if (income > 0) {
                int iy = padT + (int)(chartH * (1.0 - income / max));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[]{5, 4}, 0));
                g2.setColor(TEAL);
                g2.drawLine(padL, iy, padL + chartW, iy);
                // Income label on right
                g2.setStroke(new BasicStroke(1));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("\u20B9" + fmt0(income), padL + chartW + 2, iy + 4);
            }

            // ── Y-axis ───────────────────────────────────────────────────────
            g2.setStroke(new BasicStroke(1));
            g2.setColor(BORDER);
            g2.drawLine(padL, padT, padL, baseY);

            // ── Tooltip bar ──────────────────────────────────────────────────
            if (hoverMonth >= 0 && !tooltip.isEmpty()) {
                g2.setFont(F_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(tooltip) + 16;
                int tx = Math.min(padL + hoverMonth * groupW, padL + chartW - tw);
                int ty = padT - 2;
                g2.setColor(TEXT1);
                g2.fillRoundRect(tx, ty, tw, 18, 6, 6);
                g2.setColor(SURFACE);
                g2.drawString(tooltip, tx + 8, ty + 13);
            }
        }
    }

    // Small colored dot label
    static class DotLabel extends JComponent {
        private final Color col;
        DotLabel(Color col) {
            this.col = col;
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            setMaximumSize(new Dimension(10, 10));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(col);
            g2.fillOval(0, 2, 8, 8);
        }
    }

    // =========================================================================
    //  UI helpers
    // =========================================================================
    private JPanel surfaceCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 20, 18, 20)));
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JLabel sectionLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(F_HEADING);
        l.setForeground(TEXT1);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    g2.setColor(TEXT3);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 10, getHeight() / 2 + fm.getAscent() / 2 - 1);
                }
            }
        };
        f.setOpaque(false);
        f.setBackground(SURFACE);
        f.setForeground(TEXT1);
        f.setCaretColor(PRIMARY);
        f.setFont(F_BODY);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                ((JTextField)e.getSource()).setBorder(new CompoundBorder(
                        new LineBorder(PRIMARY, 1, true), new EmptyBorder(8, 10, 8, 10)));
            }
            @Override public void focusLost(FocusEvent e) {
                ((JTextField)e.getSource()).setBorder(new CompoundBorder(
                        new LineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
            }
        });
        return f;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(SURFACE);
        cb.setForeground(TEXT1);
        cb.setFont(F_BODY);
        cb.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(4, 6, 4, 6)));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBackground(sel ? PRIMARY_SOFT : SURFACE);
                setForeground(sel ? PRIMARY_DARK : TEXT1);
                setFont(F_BODY);
                setBorder(new EmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
    }

    private JButton primaryButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? PRIMARY_DARK
                         : getModel().isRollover() ? PRIMARY_DARK
                         : PRIMARY;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int x = (getWidth()  - fm.stringWidth(txt)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, x, y);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton secondaryButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? PRIMARY_SOFT : SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(getModel().isRollover() ? PRIMARY : TEXT2);
                g2.setFont(getFont());
                int x = (getWidth()  - fm.stringWidth(txt)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, x, y);
            }
        };
        b.setFont(F_LABEL);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton ghostButtonDark(String txt) {
        JButton b = new JButton(txt);
        b.setFont(F_LABEL);
        b.setForeground(TEXT2);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(DANGER); }
            public void mouseExited(MouseEvent e)  { b.setForeground(TEXT2); }
        });
        return b;
    }

    private JButton linkButton(String txt) {
        JButton b = new JButton(txt);
        b.setFont(F_LABEL);
        b.setForeground(PRIMARY);
        b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT1);
        t.setFont(F_SMALL);
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(PRIMARY_SOFT);
        t.setSelectionForeground(PRIMARY_DARK);
        t.setFillsViewportHeight(true);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDER);

        JTableHeader h = t.getTableHeader();
        h.setBackground(SURFACE2);
        h.setForeground(TEXT2);
        h.setFont(F_LABEL);
        h.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        h.setPreferredSize(new Dimension(0, 36));

        int[] ws = {30, 90, 90, 140, 90};
        for (int i = 0; i < ws.length; i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setBackground(sel ? PRIMARY_SOFT : row % 2 == 0 ? SURFACE : BG);
                setForeground(sel ? PRIMARY_DARK : col == 1 ? TEXT1 : col == 3 ? TEXT3 : TEXT1);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(col == 1 ? new Font("Consolas", Font.BOLD, 12) : F_SMALL);
                setHorizontalAlignment(col == 0 || col == 1 ? CENTER : LEFT);
                if (!sel && col == 2 && val != null)
                    for (int i = 0; i < CATS.length; i++)
                        if (CATS[i].equals(val.toString())) { setForeground(CCOLS[i]); break; }
                return this;
            }
        });
    }

    private Component gap(int h) { return Box.createRigidArea(new Dimension(0, h)); }
    private String fmt0(double v) { return String.format("%.0f", v); }

    private void alert(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    private JTable findTable(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTable) return (JTable) comp;
            if (comp instanceof Container) {
                JTable t = findTable((Container) comp);
                if (t != null) return t;
            }
        }
        return null;
    }

    // ─── Data class ───────────────────────────────────────────────────────────
    static class Expense {
        double amount; String category, date, reason;
        Expense(double a, String c, String d, String r) { amount=a; category=c; date=d; reason=r; }
    }

    // ─── Main ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.background", new Color(255, 255, 255));
        UIManager.put("Panel.background",      new Color(255, 255, 255));
        UIManager.put("OptionPane.messageForeground", new Color(15, 23, 42));
        SwingUtilities.invokeLater(() -> new ExpenseTracker().setVisible(true));
    }
}