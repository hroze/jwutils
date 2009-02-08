package org.opengroove.jw.jmlogo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import org.opengroove.jw.jmlogo.lang.Interpreter;
import org.opengroove.jw.jmlogo.lang.InterpreterContext;
import org.opengroove.jw.jmlogo.lang.InterpreterException;
import org.opengroove.jw.jmlogo.lang.ListToken;
import org.opengroove.jw.jmlogo.lang.Result;
import org.opengroove.jw.jmlogo.lang.StringStream;
import org.opengroove.jw.jmlogo.lang.TokenIterator;
import org.opengroove.jw.jmlogo.utils.Properties;

public class JMLogo extends MIDlet
{
    protected static final int MAX_COMMANDER_HISTORY = 20;
    /**
     * The commander history. This stores the last 20 commands run in the
     * commander.
     */
    public static Vector commanderHistory = new Vector();
    /**
     * True if JMLogo has been started, false if it has not or if it has been
     * destroyed.
     */
    public static boolean isRunning = false;
    /**
     * True if JMLogo is currently paused.
     */
    public static boolean isPaused = false;
    /**
     * The configuration store. This holds JMLogo's global settings. It's name
     * is "config".
     */
    public static RecordStore configStore;
    /**
     * The configuration properties. This is where JMLogo's global settings are
     * stored, and this is stored in record 1 (the only record, currently) of
     * the config store.
     */
    public static Properties configProperties;
    /**
     * The program store. This is the record store that represents the program
     * that is currently open. The store's name is the letter "p" (for
     * "program"), followed by the program's name.
     */
    public static RecordStore programStore;
    /**
     * The interpreter that represents the current program. The program's
     * procedures are loaded into the interpreter when the program is started,
     * as well as the library procedures.
     */
    public static Interpreter interpreter;
    /**
     * The library store. This record store represents JMLogo's library
     * functions. It is in the same format as the program stores, but it's name
     * is "library" instead of "pPROGRAMNAME".
     * 
     * When the library is being edited, this will have the same value as
     * programStore.
     */
    public static RecordStore libraryStore;
    public static LogoCanvas canvas;
    public static JMLogo midlet;
    public static TextBox commander;
    
    private static Form welcomeScreen;
    
    protected void destroyApp(boolean unconditional)
    {
        if (programStore != null)
            try
            {
                programStore.closeRecordStore();
            }
            catch (RecordStoreNotOpenException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (RecordStoreException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        if (libraryStore != null)
            try
            {
                libraryStore.closeRecordStore();
            }
            catch (RecordStoreNotOpenException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (RecordStoreException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        if (configStore != null)
            try
            {
                configStore.closeRecordStore();
            }
            catch (RecordStoreNotOpenException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (RecordStoreException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }
    
    protected void pauseApp()
    {
        destroyApp(true);
        notifyDestroyed();
    }
    
    protected void startApp() throws MIDletStateChangeException
    {
        if (midlet != null)
            throw new RuntimeException();
        midlet = this;
        SymbolUtils.init(this);
        try
        {
            RecordStore configStoreTemp = RecordStore.openRecordStore("config", false);
            configStoreTemp.closeRecordStore();
        }
        catch (Exception e)
        {
            if (e instanceof RecordStoreNotFoundException)
            {
                doFirstTimeSetup();
                return;
            }
            else
            {
                error(e, "in startApp, configStoreTemp exception while opening");
                return;
            }
        }
        doNormalSetup();
    }
    
    private void doNormalSetup()
    {
        /*
         * This method sets up everything. It's run upon startup if JMLogo has
         * been run before. It's also run after setting up JMLogo if this is the
         * first time that the user has run it.
         */
        loadBuiltInForms();
        try
        {
            libraryStore = RecordStore.openRecordStore("library", false);
        }
        catch (Exception e)
        {
            error(e, "while opening library in normal setup");
            return;
        }
        Display.getDisplay(midlet).setCurrent(welcomeScreen);
    }
    
    private void loadBuiltInForms()
    {
        welcomeScreen = new Form("Welcome to JMLogo");
        StringItem si =
            new StringItem(null,
                "Welcome to JMLogo! You can choose Help to view the getting "
                    + "started guide, or choose Next to open or create a program.");
        si.setFont(getSmallFont());
        welcomeScreen.append(si);
        welcomeScreen.addCommand(new Command("Help", Command.SCREEN, 2));
        welcomeScreen.addCommand(new Command("Next", Command.OK, 1));
        welcomeScreen.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c.getCommandType() == Command.SCREEN)
                {
                    Display.getDisplay(midlet).setCurrent(
                        new Alert("Unsupported",
                            "I haven't added the getting started guide yet.", null,
                            AlertType.ERROR), welcomeScreen);
                }
                else
                {
                    showStatusScreen("Loading program list...");
                    new Thread()
                    {
                        public void run()
                        {
                            showOpenProgramList();
                        }
                    }.start();
                }
            }
        });
    }
    
    protected void showOpenProgramList()
    {
        final List list = new List("Choose a program", List.IMPLICIT);
        String[] recordStores = RecordStore.listRecordStores();
        for (int i = 0; i < recordStores.length; i++)
        {
            if (recordStores[i].startsWith("p"))
                list.append("  " + recordStores[i].substring(1), null);
        }
        list.append("Library", null);
        list.append("New program", null);
        list.setSelectCommand(new Command("Choose", Command.ITEM, 1));
        list.addCommand(new Command("Cancel", Command.BACK, 2));
        list.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c.getCommandType() == Command.BACK)
                {
                    Display.getDisplay(midlet).setCurrent(welcomeScreen);
                    return;
                }
                else
                {
                    String chosen = list.getString(list.getSelectedIndex());
                    if (chosen.startsWith("  "))
                    {
                        openProgram("p" + chosen.substring(2));
                    }
                    else if (chosen.equalsIgnoreCase("Library"))
                    {
                        openProgram("library");
                    }
                    else if (chosen.equalsIgnoreCase("New program"))
                    {
                        showNewProgramForm();
                    }
                }
            }
        });
        Display.getDisplay(midlet).setCurrent(list);
    }
    
    protected void showNewProgramForm()
    {
        final Form form = new Form("New Program");
        final TextField nameField =
            new TextField("Name", "", 30, TextField.ANY | TextField.NON_PREDICTIVE);
        form.append(nameField);
        form.addCommand(new Command("OK", Command.OK, 1));
        form.addCommand(new Command("Cancel", Command.CANCEL, 2));
        form.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c.getCommandType() == Command.CANCEL)
                {
                    Display.getDisplay(midlet).setCurrent(welcomeScreen);
                    return;
                }
                doCreateProgram(form, nameField.getString());
            }
        });
        Display.getDisplay(midlet).setCurrent(form);
    }
    
    protected void doCreateProgram(Form backToForm, String name)
    {
        try
        {
            RecordStore temp = RecordStore.openRecordStore("p" + name, false);
            temp.closeRecordStore();
            showMessageAlert(backToForm, "A program with that name already exists");
            return;
        }
        catch (RecordStoreNotFoundException e)
        {
            /*
             * If we get here, then the record store doesn't exist. We're ok to
             * proceed, then.
             * 
             * Just a quick note, so that people don't think I'm a bad
             * programmer or something. I really hate exception-driven
             * programming, especially when an exception indicates success.
             * However, listing the record stores (which would enable me to
             * iterate over them and check for existence, thereby avoiding
             * exception-driven programming here) has proved to be very
             * expensive, in terms of the time it takes to execute. I've done
             * exception-based programming here only in the interest of
             * performance, and not because it's a good coding practice (it
             * isn't).
             */
        }
        catch (Exception e)
        {
            error(e, "while testing for rms existence when creating a new program");
            return;
        }
        try
        {
            RecordStore localProgramStore =
                RecordStore.openRecordStore("p" + name, true);
            Properties props = new Properties();
            byte[] bytes = props.write();
            localProgramStore.addRecord(bytes, 0, bytes.length);
            localProgramStore.closeRecordStore();
            openProgram("p" + name);
        }
        catch (Exception e)
        {
            error(e, "while creating new program record store");
            return;
        }
    }
    
    private void showMessageAlert(Form form, String message)
    {
        Alert alert = new Alert("JMLogo", message, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        Display.getDisplay(midlet).setCurrent(alert, form);
    }
    
    protected void openProgram(final String programName)
    {
        showStatusScreen("Opening program...");
        new Thread()
        {
            public void run()
            {
                if (programStore != null)
                {
                    error(new Exception(),
                        "Attempt to open a program when one is already open");
                    return;
                }
                try
                {
                    programStore = RecordStore.openRecordStore(programName, false);
                    interpreter = new Interpreter();
                    interpreter.installDefaultCommands();
                    if (!programStore.equals(libraryStore))
                    {
                        loadProceduresIntoInterpreter(libraryStore, interpreter);
                    }
                    loadProceduresIntoInterpreter(programStore, interpreter);
                    canvas = new LogoCanvas();
                    canvas.init();
                    interpreter.installTurtleCommands(canvas);
                    loadCanvasCommands();
                    loadCommander();
                    addCanvasCommands();
                    Display.getDisplay(midlet).setCurrent(canvas);
                }
                catch (Exception e)
                {
                    error(e, "while opening a program");
                    return;
                }
            }
        }.start();
    }
    
    protected void loadCommander()
    {
        commander =
            new TextBox("Commander", "", 1024, TextField.ANY | TextField.NON_PREDICTIVE);
        commander.addCommand(new Command("Symbol", Command.OK, 1));
        commander.addCommand(new Command("Newline", Command.CANCEL, 2));
        commander.addCommand(new Command("Run", Command.SCREEN, 3));
        commander.addCommand(new Command("Cancel", Command.CANCEL, 4));
        commander.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c.getLabel().equalsIgnoreCase("Symbol"))
                {
                    SymbolUtils.showSymbolForm(commander);
                }
                else if (c.getLabel().equalsIgnoreCase("Cancel"))
                {
                    Display.getDisplay(midlet).setCurrent(canvas);
                }
                else if (c.getLabel().equalsIgnoreCase("Run"))
                {
                    executeCommanderContents();
                }
                else
                // if (c.getLabel().equalsIgnoreCase("Newline"))
                {
                    commander.insert(new char[] { '\n' }, 0, 1, commander
                        .getCaretPosition());
                }
            }
        });
    }
    
    protected void executeCommanderContents()
    {
        removeCanvasCommands();
        Display.getDisplay(midlet).setCurrent(canvas);
        new Thread()
        {
            public void run()
            {
                try
                {
                    interpret(commander.getString());
                    commanderHistory.addElement(commander.getString());
                    if (commanderHistory.size() > MAX_COMMANDER_HISTORY)
                        commanderHistory.removeElementAt(0);
                    commander.setString("");
                    addCanvasCommands();
                }
                catch (InterpreterException e)
                {
                    Alert alert =
                        new Alert("JMLogo", "Logo error: " + e.getMessage(), null,
                            AlertType.ERROR);
                    Display.getDisplay(midlet).setCurrent(alert, commander);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    error(e, "While running contents of commander");
                }
            }
        }.start();
    }
    
    protected void interpret(String string)
    {
        StringStream stream = new StringStream("[" + string + "]");
        ListToken token = interpreter.parseToList(stream);
        TokenIterator iterator = new TokenIterator(token);
        Result result =
            interpreter.evaluate(iterator, new InterpreterContext(interpreter, null));
        if (result == null)
            return;
        if (result.getType() == Result.TYPE_IN_LINE)
            throw new InterpreterException("I don't know what to do with "
                + interpreter.toReadable(result.getValue(), 64));
        if (result.getType() == Result.TYPE_OUTPUT)
            throw new InterpreterException("Output can only be used in a procedure");
        /*
         * TODO: this currently allows stop statements. This isn't allowed by
         * Logo (and in particular, MSWLogo shows an error when it happens), but
         * it would probably be a useful command to have, so that commander
         * contents could be stopped from executing programmatically.
         */
    }
    
    public static void removeCanvasCommands()
    {
        for (int i = 0; i < canvasCommands.length; i++)
        {
            canvas.removeCommand(canvasCommands[i]);
        }
    }
    
    public static void addCanvasCommands()
    {
        for (int i = 0; i < canvasCommands.length; i++)
        {
            canvas.addCommand(canvasCommands[i]);
        }
    }
    
    private static Command[] canvasCommands;
    
    protected void loadCanvasCommands()
    {
        canvasCommands = new Command[7];
        canvasCommands[0] = new Command("Run", Command.SCREEN, 1);
        canvasCommands[1] = new Command("Close", Command.SCREEN, 2);
        canvasCommands[2] = new Command("Edit", Command.SCREEN, 3);
        canvasCommands[3] = new Command("Copy from", Command.SCREEN, 4);
        canvasCommands[4] = new Command("Set screencolor", Command.SCREEN, 5);
        canvasCommands[5] = new Command("Set pencolor", Command.SCREEN, 6);
        canvasCommands[6] = new Command("Delete procedure", Command.SCREEN, 7);
        canvas.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c == canvasCommands[0])// Run
                {
                    Display.getDisplay(midlet).setCurrent(commander);
                }
                else if (c == canvasCommands[1])// Close
                {
                    
                }
                else if (c == canvasCommands[2])// Edit
                {
                    showChooseEditProcList();
                }
                else if (c == canvasCommands[3])// Copy from
                {
                    
                }
                else if (c == canvasCommands[4])// Set screencolor
                {
                    
                }
                else if (c == canvasCommands[5])// Set pencolor
                {
                    
                }
                else if (c == canvasCommands[6])// Delete procedure
                {
                    showChooseDeleteProcList();
                }
            }
        });
    }
    
    protected void showChooseDeleteProcList()
    {
        String[] procedures = listProgramProcedures();
        Image[] nullImages = new Image[procedures.length];
        final List list =
            new List("Choose a procedure to delete", List.IMPLICIT, procedures,
                nullImages);
        final Command chooseCommand = new Command("Edit", Command.ITEM, 1);
        list.addCommand(new Command("Cancel", Command.SCREEN, 2));
        list.setSelectCommand(chooseCommand);
        list.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c == chooseCommand)
                {
                    String selection = list.getString(list.getSelectedIndex());
                    showConfirmDeleteForm(selection);
                }
                else
                {
                    Display.getDisplay(midlet).setCurrent(canvas);
                }
            }
        });
        Display.getDisplay(midlet).setCurrent(list);
    }
    
    protected void showConfirmDeleteForm(String selection)
    {
        Form form = new Form("Are you sure?");
        form.append("Are you sure you want to delete the procedure " + selection + "?");
        form.addCommand(new Command("Yes", Command.OK, 2));
        form.addCommand(new Command("No", Command.CANCEL, 1));
    }
    
    protected void showChooseEditProcList()
    {
        final List list = new List("Choose a procedure to edit", List.IMPLICIT);
        String[] procedureNames = listProgramProcedures();
        for (int i = 0; i < procedureNames.length; i++)
        {
            list.append("  " + procedureNames[i], null);
        }
        list.append("New procedure", null);
        final Command chooseCommand = new Command("Edit", Command.ITEM, 1);
        list.addCommand(new Command("Cancel", Command.SCREEN, 2));
        list.setSelectCommand(chooseCommand);
        list.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c == chooseCommand)
                {
                    String selection = list.getString(list.getSelectedIndex());
                    if (selection.startsWith("  "))
                    {
                        openProcedureEditor(selection.substring(2));
                    }
                    else
                    {
                        showCreateProcedureForm();
                    }
                }
                else
                {
                    Display.getDisplay(midlet).setCurrent(canvas);
                }
            }
        });
        Display.getDisplay(midlet).setCurrent(list);
    }
    
    protected void showCreateProcedureForm()
    {
        final Form form = new Form("New Procedure");
        final TextField nameField =
            new TextField("Name", "", 30, TextField.ANY | TextField.NON_PREDICTIVE);
        form.append(nameField);
        form.append("You can enter the procedure's arguments later.");
        form.addCommand(new Command("OK", Command.OK, 1));
        form.addCommand(new Command("Cancel", Command.CANCEL, 2));
        form.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                if (c.getCommandType() == Command.CANCEL)
                {
                    Display.getDisplay(midlet).setCurrent(canvas);
                    return;
                }
                doCreateProcedure(form, nameField.getString());
            }
        });
        Display.getDisplay(midlet).setCurrent(form);
    }
    
    protected void doCreateProcedure(Form form, String string)
    {
        // TODO Auto-generated method stub
        
    }
    
    protected void openProcedureEditor(String substring)
    {
        // TODO Auto-generated method stub
        
    }
    
    public static String[] listProgramProcedures()
    {
        try
        {
            RecordEnumeration e = programStore.enumerateRecords(new RecordFilter()
            {
                
                public boolean matches(byte[] candidate)
                {
                    return candidate.length > 0 && candidate[0] == 'p';
                }
            }, null, false);
            Vector names = new Vector();
            while (e.hasNextElement())
            {
                byte[] bytes = e.nextRecord();
                int bytesInName = bytes[1];
                int index = 2;
                StringBuffer nameBuffer = new StringBuffer();
                for (int i = 0; i < bytesInName; i++)
                {
                    nameBuffer.append((char) bytes[index++]);
                }
                names.addElement(nameBuffer.toString());
            }
            e.destroy();
            String[] result = new String[names.size()];
            names.copyInto(result);
            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException("while listing records in listProgramProcedures");
        }
    }
    
    /**
     * Loads the procedures in the record store specified into the interpreter
     * specified.
     * 
     * @param store
     *            The store to load from. All records that start with a "p" will
     *            be loaded. The byte after "p" is the length of the name of the
     *            procedure. That many bytes are then present, which make up the
     *            name. The byte after is the number of chars in the argument
     *            string. The argument string then follows. The rest of the
     *            record is the contents of the procedure itself. This should
     *            not end with "end".
     * @param it
     *            The interpreter to load the records into
     */
    protected static void loadProceduresIntoInterpreter(RecordStore store,
        Interpreter it)
    {
        try
        {
            RecordEnumeration e = store.enumerateRecords(new RecordFilter()
            {
                
                public boolean matches(byte[] candidate)
                {
                    return candidate.length > 0 && candidate[0] == 'p';
                }
            }, null, false);
            while (e.hasNextElement())
            {
                loadProcedureIntoInterpreter(e.nextRecord(), it);
            }
            e.destroy();
        }
        catch (Exception e)
        {
            throw new RuntimeException(
                "while loading rms procedures or similar, not open");
        }
    }
    
    /**
     * Loads the procedure specified.
     * 
     * TODO: macros are not supported by this yet. Add support for them.
     * 
     * @param bytes
     * @param it
     */
    public static void loadProcedureIntoInterpreter(byte[] bytes, Interpreter it)
    {
        // skip the first byte, which should be "p"
        int bytesInName = bytes[1];
        int index = 2;
        StringBuffer nameBuffer = new StringBuffer();
        for (int i = 0; i < bytesInName; i++)
        {
            nameBuffer.append((char) bytes[index++]);
        }
        int bytesInVarDef = bytes[index++];
        StringBuffer varDefBuffer = new StringBuffer();
        for (int i = 0; i < bytesInVarDef; i++)
        {
            varDefBuffer.append((char) bytes[index++]);
        }
        String content = new String(bytes, index, bytes.length - index);
        String name = nameBuffer.toString();
        String varDef = varDefBuffer.toString();
        it.define(name + " " + varDef, content, false);
    }
    
    private Font getSmallFont()
    {
        return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    }
    
    private void doFirstTimeSetup()
    {
        /*
         * This is called the first time that JMLogo is run, and sets up the
         * config store and the library store, and loads the included libraries
         * into the library store.
         */
        Form form = new Form("JMLogo");
        StringItem si =
            new StringItem(null, "Welcome to JMLogo! \n" + "\n"
                + "JMLogo needs to set some stuff up before "
                + "you start programming. Press next when you're ready.");
        si.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        form.append(si);
        form.addCommand(new Command("Next", Command.SCREEN, 1));
        form.setCommandListener(new CommandListener()
        {
            
            public void commandAction(Command c, Displayable d)
            {
                showStatusScreen("Setting everything up...");
                new Thread()
                {
                    public void run()
                    {
                        beginFirstTimeSetup();
                    }
                }.start();
            }
        });
        Display.getDisplay(this).setCurrent(form);
    }
    
    protected void beginFirstTimeSetup()
    {
        try
        {
            /*
             * First, we need to create the config and library record stores.
             */
            System.out.println("loading stores");
            RecordStore localConfigStore = RecordStore.openRecordStore("config", true);
            RecordStore localLibraryStore =
                RecordStore.openRecordStore("library", true);
            Properties localConfigProps = new Properties();
            byte[] localConfigPropsBytes = localConfigProps.write();
            localConfigStore.addRecord(localConfigPropsBytes, 0,
                localConfigPropsBytes.length);
            localConfigStore.closeRecordStore();
            Properties localLibraryProps = new Properties();
            byte[] llpBytes = localLibraryProps.write();
            localLibraryStore.addRecord(llpBytes, 0, llpBytes.length);
            System.out.println("loading library list input");
            DataInputStream lListIn =
                new DataInputStream(getClass().getResourceAsStream(
                    "/library/files.list"));
            System.out.println("loading library into record store");
            while (true)
            {
                try
                {
                    String name = lListIn.readUTF();
                    if (name == null)
                        break;
                    if (name.endsWith("/"))
                        /*
                         * Shouldn't happen, but we'll be careful anyway
                         */
                        continue;
                    System.out.println("loading library function " + name);
                    InputStream in = getClass().getResourceAsStream("/library/" + name);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write('p');
                    copy(in, baos);
                    in.close();
                    byte[] bytes = baos.toByteArray();
                    System.out.println("writing library function to record store");
                    localLibraryStore.addRecord(bytes, 0, bytes.length);
                }
                catch (EOFException e)
                {
                    break;
                }
            }
            System.out.println("closing library record store");
            localLibraryStore.closeRecordStore();
            /*
             * The initial record stores have been created. Now we'll proceed as
             * if the user had just started up the application.
             */
            doNormalSetup();
        }
        catch (Exception e)
        {
            error(e, "While in beginFirstTimeSetup");
        }
    }
    
    public static void error(Throwable e, String info)
    {
        Form f = new Form("JMLogo Error");
        f.append("An error has occured in JMLogo "
            + "(scroll down for contact info)...\n");
        f.append(e.getClass().getName() + "\n");
        f.append(e.getMessage() + "\n");
        if (info != null)
            f.append(info);
        f.append("Report this to support@opengroove.org");
        Display.getDisplay(midlet).setCurrent(f);
        return;
    }
    
    public static void error(Throwable t)
    {
        error(t, null);
    }
    
    /**
     * Copies the contents of one stream to another. Bytes from the source
     * stream are read until it is empty, and written to the destination stream.
     * Neither the source nor the destination streams are flushed or closed.
     * 
     * @param in
     *            The source stream
     * @param out
     *            The destination stream
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void copy(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[8192];
        int amount;
        while ((amount = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, amount);
        }
    }
    
    public static void showStatusScreen(String message)
    {
        Form form = new Form("JMLogo");
        form.append(message);
        Display.getDisplay(midlet).setCurrent(form);
    }
    
}
