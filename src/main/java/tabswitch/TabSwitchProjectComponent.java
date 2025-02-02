/*
 * Copyright (c) 2008-2011 by Fuhrer Engineering AG, CH-2504 Biel/Bienne, Switzerland & Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tabswitch;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.BitSet;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;


import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import tabswitch.component.Components;

public class TabSwitchProjectComponent extends AbstractProjectComponent { // implements KeyEventDispatcher {

  private final BitSet modifiers = new BitSet();
  private final JList list;
  private final PopupChooserBuilder builder;

  private JBPopup popup;
  private int trigger = 0;
  private boolean movingUp;
  private boolean isTriggeredWithShift;

  public TabSwitchProjectComponent(Project project) {
    super(project);

    JLabel pathLabel = Components.newPathLabel();
    this.list = Components.newList(project, pathLabel);
    this.builder = new PopupChooserBuilder(list);

    this.builder
      .setTitle("Open files")
      .setMovable(true)
      .setAutoselectOnMouseMove(false)
      .setSouthComponent(Components.newListFooter(pathLabel))
      .setItemChoosenCallback(new Runnable() {
        @Override
        public void run() {
          closeAndOpenSelectedFile();
        }
      });
  }

  public static TabSwitchProjectComponent getHandler(Project project) {
    return project.getComponent(TabSwitchProjectComponent.class);
  }

  /*
  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    boolean consumed = true;
    if (popup != null && popup.isDisposed()) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
      consumed = false;
    } else if ((event.getID() == KeyEvent.KEY_RELEASED) && modifiers.get(event.getKeyCode())) {
      closeAndOpenSelectedFile();
    } else if (event.getID() == KeyEvent.KEY_PRESSED) {
      int keyCode = event.getKeyCode();
      if (keyCode == trigger) {
        moveOnTrigger(event.isShiftDown());
      } else {
        switch (keyCode) {
          case KeyEvent.VK_UP:
            moveUp();
            break;
          case KeyEvent.VK_DOWN:
            moveDown();
            break;
          case KeyEvent.VK_ENTER:
            closeAndOpenSelectedFile();
            break;
          case KeyEvent.VK_SHIFT:
          case KeyEvent.VK_CONTROL:
          case KeyEvent.VK_ALT:
          case KeyEvent.VK_ALT_GRAPH:
          case KeyEvent.VK_META:
            break;
          default:
            close();
            break;
        }
      }
    }
    return consumed;
  }*/

  public void show(KeyEvent event, boolean moveUp, boolean moveOnShow, List<VirtualFile> files) {
    if (cannotShow(files)) return;

    if (popup != null) popup.dispose();

    prepareListWithFiles(files);

    popup = builder.createPopup();

    trigger = event.getKeyCode();

    modifiers.set(KeyEvent.VK_CONTROL, event.isControlDown());
    modifiers.set(KeyEvent.VK_META, event.isMetaDown());
    modifiers.set(KeyEvent.VK_ALT, event.isAltDown());
    modifiers.set(KeyEvent.VK_ALT_GRAPH, event.isAltGraphDown());

    movingUp = moveUp;
    isTriggeredWithShift = event.isShiftDown();

    // KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);

    popup.showCenteredInCurrentWindow(myProject);

    if (moveOnShow) {
      move(movingUp);
    }
  }

  private boolean cannotShow(List<VirtualFile> files) {
    return files.isEmpty() || popup != null && popup.isVisible();
  }

  private void prepareListWithFiles(final List<VirtualFile> files) {
    list.setModel(new AbstractListModel() {
      @Override
      public int getSize() {
        return files.size();
      }

      @Override
      public Object getElementAt(int index) {
        return files.get(index);
      }
    });

    list.setVisibleRowCount(files.size());
  }

  private void moveOnTrigger(boolean isShiftDown) {
    boolean reverse = isTriggeredWithShift != isShiftDown;
    boolean up = reverse ^ movingUp;
    move(up);
  }

  private void moveUp() {
    move(true);
  }

  private void moveDown() {
    move(false);
  }

  private void move(boolean up) {
    int offset = up ? -1 : 1;
    int size = list.getModel().getSize();
    list.setSelectedIndex((list.getSelectedIndex() + size + offset) % size);
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public void closeAndOpenSelectedFile() {
    close();
    openSelectedFile();
  }

  private void close() {
    disposePopup();
    removeMouseListeners();
    // KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
  }

  private void openSelectedFile() {
    VirtualFile file = (VirtualFile) list.getSelectedValue();
    if (file != null && file.isValid()) {
      FileEditorManager.getInstance(myProject).openFile(file, true, true);
    }
  }

  private void disposePopup() {
    if (popup != null) {
      popup.cancel();
      popup.dispose();
      popup = null;
    }
  }

  /**
   * Workaround for MouseListener leak added in PopupChooserBuilder.createPopup().
   */
  private void removeMouseListeners() {
    for (MouseListener listener : list.getMouseListeners()) {
      removeMouseListener(listener);
    }
  }

  private void removeMouseListener(MouseListener listener) {
    if (listener.getClass().getName().startsWith("com.intellij.openapi.ui.popup.PopupChooserBuilder")) {
      list.removeMouseListener(listener);
    }
  }
}
