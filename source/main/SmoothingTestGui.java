/**
 * Copyright © 2014-2015 Paolo Simonetto
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package main;

import ocotillo.gui.GraphCanvas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class SmoothingTestGui extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JToolBar toolBar = new JToolBar();
    private final JPanel mainArea = new JPanel();
    private Thread currentThread;

    /**
     * Runs a new instance of smoothing test GUI.
     */
    public static void showGui() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                SmoothingTestGui gui = new SmoothingTestGui();
                gui.setVisible(true);
            }
        });

    }

    /**
     * Constructs a Smoothing Tests window.
     */
    private SmoothingTestGui() {
        setTitle("Smoothing Tests");
        setSize(1300, 1000);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        toolBar.setFloatable(false);
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        mainArea.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(toolBar);
        add(scrollPane, BorderLayout.WEST);
        add(mainArea, BorderLayout.CENTER);

//        addTest(new SmoothingTest.TriangleSingle());
//        addTest(new SmoothingTest.ConcaveSingle());
//        addTest(new SmoothingTest.ConcaveFlexibleSingle());
        addTest(new SmoothingTest.AmebaFD());
        addTest(new SmoothingTest.AmebaConstrFD());
        addTest(new SmoothingTest.SpiralFD());
        addTest(new SmoothingTest.SpiralConstrFD());
        addTest(new SmoothingTest.SingleSetFixed());
        addTest(new SmoothingTest.SingleSetMoveable());
        addTest(new SmoothingTest.DoubleSetFixed());
        addTest(new SmoothingTest.DoubleSetMoveable());
        addTest(new SmoothingTest.Imdb20Fixed());
        addTest(new SmoothingTest.Imdb20FixedIndep());
        addTest(new SmoothingTest.Imdb20Moveable());
        addTest(new SmoothingTest.Imdb20MoveableIndep());
        addTest(new SmoothingTest.ManhattanBubble());
        addTest(new SmoothingTest.UntangledFixed());
        addTest(new SmoothingTest.Untangled());
        addTest(new SmoothingTest.UntangledSmall());
        addTest(new SmoothingTest.GeneralEuler());
        addTest(new SmoothingTest.SetVisualizerFixed());
        addTest(new SmoothingTest.SetVisualizerMovable());
        addTest(new SmoothingTest.EulerForce1());
        addTest(new SmoothingTest.EulerForce2());
        addTest(new SmoothingTest.EulerForce3sets(1));
//        addTest(new SmoothingTest.EulerForce3sets(2));
        addTest(new SmoothingTest.EulerForce3sets(3));
//        addTest(new SmoothingTest.EulerForce3sets(4));
        addTest(new SmoothingTest.EulerForce3sets(5));
//        addTest(new SmoothingTest.EulerForce3sets(6));
        addTest(new SmoothingTest.EulerForce3sets(7));
//        addTest(new SmoothingTest.EulerForce3sets(8));
        addTest(new SmoothingTest.EulerForce3sets(9));
//        addTest(new SmoothingTest.EulerForce3sets(10));
        addTest(new SmoothingTest.EulerForce4sets(1));
//        addTest(new SmoothingTest.EulerForce4sets(2));
        addTest(new SmoothingTest.EulerForce4sets(3));
//        addTest(new SmoothingTest.EulerForce4sets(4));
        addTest(new SmoothingTest.EulerForce4sets(5));
//        addTest(new SmoothingTest.EulerForce4sets(6));
        addTest(new SmoothingTest.EulerForce4sets(7));
//        addTest(new SmoothingTest.EulerForce4sets(8));
        addTest(new SmoothingTest.EulerForce4sets(9));
//        addTest(new SmoothingTest.EulerForce4sets(10));
        addTest(new SmoothingTest.EulerForce5sets(1));
        addTest(new SmoothingTest.EulerForce5sets(3));
//        addTest(new SmoothingTest.EulerForce5sets(4));
        addTest(new SmoothingTest.EulerForce5sets(5));
//        addTest(new SmoothingTest.EulerForce5sets(6));
        addTest(new SmoothingTest.EulerForce5sets(7));
        addTest(new SmoothingTest.EulerForce5sets(9));
//        addTest(new SmoothingTest.Euler3runtime(0.69));
//        addTest(new SmoothingTest.Euler3runtime(0.83));
//        addTest(new SmoothingTest.Euler3runtime(1));
//        addTest(new SmoothingTest.Euler3runtime(1.2));
//        addTest(new SmoothingTest.Euler3runtime(1.44));
//        addTest(new SmoothingTest.Euler4runtime(0.69));
//        addTest(new SmoothingTest.Euler4runtime(0.83));
//        addTest(new SmoothingTest.Euler4runtime(1));
//        addTest(new SmoothingTest.Euler4runtime(1.2));
//        addTest(new SmoothingTest.Euler4runtime(1.44));
//        addTest(new SmoothingTest.Euler5runtime(0.69));
//        addTest(new SmoothingTest.Euler5runtime(0.83));
//        addTest(new SmoothingTest.Euler5runtime(1));
//        addTest(new SmoothingTest.Euler5runtime(1.2));
//        addTest(new SmoothingTest.Euler5runtime(1.44));
    }

    private void addTest(final SmoothingTest test) {
        StringBuilder buttonText = new StringBuilder();
        buttonText.append("<html>");
        buttonText.append(test.getName()).append("<br>");
        buttonText.append("<small><span style=\"font-weight:normal;\">").append(test.getDescription()).append("</span></small>");
        buttonText.append("</html>");

        JButton button = new JButton(buttonText.toString());
        button.setPreferredSize(new Dimension(250, 50));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        toolBar.add(button);

        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (currentThread != null) {
                    currentThread.interrupt();
                }

                test.reset();
                mainArea.removeAll();
                mainArea.add(test.comment, BorderLayout.SOUTH);
                mainArea.add(new OptionBar(test), BorderLayout.NORTH);
                mainArea.add(new GraphCanvas(test.getGraph()), BorderLayout.CENTER);
                mainArea.validate();
            }
        });
    }

    private class OptionBar extends JPanel {

        private static final long serialVersionUID = 1L;
        private final SmoothingTest test;

        public OptionBar(final SmoothingTest test) {
            super();
            this.test = test;

            addIterationField();
            addDistanceField();

            if (test.fullOptions) {
                addDepButtons();
                addMovButtons();
                addSepButton();
            }

            final JButton startButton = new JButton("Start");
            startButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (currentThread != null) {
                        currentThread.interrupt();
                    }
                    currentThread = new Thread(test);
                    currentThread.start();
                    startButton.setEnabled(false);
                }
            });

            this.add(startButton);

        }

        private void addIterationField() {
            JLabel label = new JLabel("Iterations:");
            final JFormattedTextField field = new JFormattedTextField(NumberFormat.INTEGER_FIELD);
            field.setValue(test.iterations);
            field.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent pce) {
                    test.iterations = ((Number)field.getValue()).intValue();
                }
            });

            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.black));
            panel.add(label);
            panel.add(field);

            this.add(panel);
        }
        
        private void addDistanceField(){
            JLabel label = new JLabel("Distance:");
            final JFormattedTextField field = new JFormattedTextField(new DecimalFormat("#.##"));
            field.setValue(test.distance);
            field.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent pce) {
                    test.distance = ((Number)field.getValue()).doubleValue();
                }
            });

            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.black));
            panel.add(label);
            panel.add(field);

            this.add(panel);            
        }

        private void addDepButtons() {
            JRadioButton depButton = new JRadioButton("Dep");
            JRadioButton indButton = new JRadioButton("Ind");

            ButtonGroup group = new ButtonGroup();
            group.add(depButton);
            group.add(indButton);

            if (test.ind) {
                indButton.setSelected(true);
            } else {
                depButton.setSelected(true);
            }

            depButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    test.ind = false;
                }
            });

            indButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    test.ind = true;
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            buttonPanel.add(depButton);
            buttonPanel.add(indButton);

            this.add(buttonPanel);
        }

        private void addMovButtons() {
            JRadioButton fixButton = new JRadioButton("Fix");
            JRadioButton movButton = new JRadioButton("Mov");

            ButtonGroup group = new ButtonGroup();
            group.add(fixButton);
            group.add(movButton);

            if (test.mov) {
                movButton.setSelected(true);
            } else {
                fixButton.setSelected(true);
            }

            fixButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    test.mov = false;
                }
            });

            movButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    test.mov = true;
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            buttonPanel.add(fixButton);
            buttonPanel.add(movButton);

            this.add(buttonPanel);
        }

        private void addSepButton() {
            final JCheckBox sepButton = new JCheckBox("Sep");

            if (test.sep) {
                sepButton.setSelected(true);
            } else {
                sepButton.setSelected(false);
            }

            sepButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    test.sep = sepButton.isSelected();
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            buttonPanel.add(sepButton);

            this.add(buttonPanel);
        }

    }

}
