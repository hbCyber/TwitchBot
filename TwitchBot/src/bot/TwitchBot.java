package bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.joda.time.DateTime;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.output.OutputIRC;

public class TwitchBot extends ListenerAdapter
{
    // Special charaters
    // ♦ = &diams;
    // ✪ = &#10026;
    
    // Constant(s)
    // -- Change these:
    private final static String IRC_NICKNAME = "BOT_NICKNAME";
    private final static String IRC_SERVER = "irc.twitch.tv";
    private final static String IRC_CHANNEL = "#NAME_OF_CHANNEL";
    private final static String IRC_PASSWORD = "oauth:YOUR_OAUTH_KEY";
    private final static String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm z";
    private final static String IN_GAME_TIME_FORMAT = "mm:ss";
    private final static String NICK_STREAMER = "NAME_OF_STREAMER";
    private final static String NICK_MASTER = "NAME_OF_MASTER";    
    private final static String COINS_NAME = "MelonCoins";    
    private final static int COINS_INITIAL = 100;
    private final static int COINS_GEM_WIN = 200;
    private final static int COINS_DODGE_DODGE_WIN = 50;
    private final static int COINS_DODGE_DUEL_WIN = 400;
    private final static int COINS_POOR_THRESHOLD = 20;
    private final static double COINS_BONUS_MULTIPLIER = 1.25;
    
    // -- Technical
    private final static long MESSAGE_DELAY_MS = 2200;
    private final static long BACKUP_DELAY_MS = (60000 * 20);
    
    private final static long DODGE_BET_CLOSE_DELAY_S = 45;
    private final static long GEM_BET_CLOSE_DELAY_S = 45;
    private final static long GAME_BET_CLOSE_DELAY_S = 60;
    private final static long ROLL_CLOSE_DELAY_S = 45;
    
    private final static String BACKUP_FILE = "NAME_OF_BACKUP_FILE.properties";
    
    private final static Integer DIVISION_PRECISION = 32;
    
    // Member(s)
    private final DateFormat timestampFormat_;
    private final DateFormat inGameTimeFormat_;
    private final DecimalFormat coinFormat_;
    private MessageTask messageTask_;
    
    // -- Help
    private String helpUrl_;
    
    // -- BotMod
    private final Set<String> botMod_;
    
    // -- Coins
    private final Map<String, Integer> coins_;
    
    // -- Gem
    private Boolean gemEnabled_;
    private Boolean gemBetsOpened_;
    private Long gemBetPeriod_;
    private Timer gemBetTimer_;
    private final Map<String, DateTime> gemBets_;
    
    // -- Dodge
    private Boolean dodgeEnabled_;
    private Boolean dodgeBetsOpened_;
    private Long dodgeBetPeriod_;
    private Timer dodgeBetTimer_;
    private String dodgeName_;
    private final Map<String, Boolean> dodgeBets_;
    
    // -- Game
    private Boolean gameEnabled_;
    private Boolean gameBetsOpened_;
    private Long gameBetPeriod_;
    private Timer gameBetTimer_;
    private final Map<String, Boolean> gameBetsWinLose_;
    private final Map<String, Integer> gameBetsAmount_;
    private String gameBonusNick_;
    
    // -- Roll
    private Boolean rollEnabled_;
    private Long rollBetPeriod_;
    private Timer rollTimer_;
    private String rollInitiatorNick_;
    private Integer rollBetSize_;
    private final List<String> rollPlayers_;
    private final Set<String> rollPoorPlayers_;
    
    // -- BigPlay
    private Integer bigPlayCount_;
    private long latestBigPlayTimestamp_;
    private String latestBigPlayNick_;
    private String latestBigPlayDescription_;   
    
    // -- ShitList
    private final Set<String> shitList_;
        
    // Constructor(s)
    public TwitchBot()
    {
        // Initialize member(s)
        timestampFormat_ = new SimpleDateFormat(TIMESTAMP_FORMAT);
        timestampFormat_.setTimeZone(TimeZone.getTimeZone("EST"));
        
        inGameTimeFormat_ = new SimpleDateFormat(IN_GAME_TIME_FORMAT);
        
        coinFormat_ = new DecimalFormat();
        coinFormat_.setGroupingUsed(true);
        coinFormat_.setMinimumFractionDigits(0);
        coinFormat_.setMaximumFractionDigits(0);
        
        // -- Help
        helpUrl_ = "";
        
        // -- BotMod
        botMod_ = new HashSet<>();
        
        // -- Coins
        coins_ = new HashMap<>();
        
        // -- Gem
        gemEnabled_ = false;
        gemBetsOpened_ = false;
        gemBets_ = new HashMap<>();
        gemBetPeriod_ = GEM_BET_CLOSE_DELAY_S;
        
        // -- Dodge
        dodgeEnabled_ = false;
        dodgeBetsOpened_ = false;
        dodgeName_ = null;
        dodgeBets_ = new HashMap<>();
        dodgeBetPeriod_ = DODGE_BET_CLOSE_DELAY_S;
        
        // -- Game
        gameEnabled_ = false;
        gameBetsOpened_ = false;
        gameBetsWinLose_ = new HashMap<>();
        gameBetsAmount_ = new HashMap<>();
        gameBetPeriod_ = GAME_BET_CLOSE_DELAY_S;        
        
        // -- Roll
        rollEnabled_ = false;
        rollInitiatorNick_ = "";
        rollBetSize_ = 0;
        rollPlayers_ = new ArrayList<>();
        rollPoorPlayers_ = new HashSet<>();
        rollBetPeriod_ = ROLL_CLOSE_DELAY_S;
        
        // -- BigPlay
        bigPlayCount_ = 0;
        latestBigPlayTimestamp_ = new Date().getTime();
        latestBigPlayNick_ = "";
        latestBigPlayDescription_ = "";
        
        // -- ShitList
        shitList_ = new HashSet<>();
        
        // Load values from disk        
        loadBackup();
        
        // Start backup process
        BackupTask backupTask = new BackupTask(this);
        Timer messageTimer = new Timer(true);
        messageTimer.scheduleAtFixedRate(
            backupTask, 0, BACKUP_DELAY_MS);                
    }
    
    // Event Handling   
    @Override
    public void onConnect(ConnectEvent event)
        throws Exception
    {
        // Connection successful
        System.out.println("\n** Connected to " 
            + event.getBot().getConfiguration().getServerHostname() + "\n");                       
    }
    
    @Override
    public void onDisconnect(DisconnectEvent event)
        throws Exception
    {
        // Disconnected
        System.out.println("\n** Disconnected from " 
            + event.getBot().getConfiguration().getServerHostname() + "\n");
        
        // Perform one last backup
        saveBackup();
    }
    
    @Override
    public void onJoin(JoinEvent event)
        throws Exception
    {
        if (event.getChannel().getName().equalsIgnoreCase(IRC_CHANNEL))
        {
            // Display message upon joining channel
            if (event.getUser().getNick().equalsIgnoreCase(IRC_NICKNAME))
            {
                // Joined the channel successfully.
                outputMessage("&diams;&diams; The " + IRC_NICKNAME + " cometh.");

                // Perform test(s) here...            
            }

            // Display message if shitlisted user joins
            if (shitListContains(event.getUser().getNick()))
            {
                shitListGreet(event.getUser().getNick());
            }     
            
            // Display message if author joins
            if (event.getUser().getNick().equalsIgnoreCase(NICK_MASTER))
            {
                //outputMessage("&diams;&diams; Welcome back, master " + NICK_AUTHOR + ".");
            }

            // Greet the leader(s)
            List<String> currentLeaderNicks = new ArrayList<>();
            Integer maxBalance = coinsGetLeaders(currentLeaderNicks);
            if (currentLeaderNicks.contains(
                event.getUser().getNick().toLowerCase()))
            {
                outputMessage("&diams;&diams; ATTENTION EVERYONE, "
                    + COINS_NAME + " magnate " + event.getUser().getNick().toUpperCase()
                    + " has joined the stream with his " + maxBalance
                    + " " + COINS_NAME + ".");
            }
        }
    }
    
    @Override
    public void onMessage(MessageEvent event)
        throws Exception
    {
        // Split message into words/commands
        StringTokenizer tokenizer = new StringTokenizer(event.getMessage());
        List<String> tokens = new ArrayList<>();
        while (tokenizer.hasMoreTokens())
        {
            tokens.add(tokenizer.nextToken());
        }
        
        // Abort on empty commands
        if (tokens.size() < 1) return;
        
        // -- Help
        if (tokens.get(0).equalsIgnoreCase("!help"))
        {
            handleHelpCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),                
                tokens); 
        }
        
        // -- BotMod
        if (tokens.get(0).equalsIgnoreCase("!botmod"))
        {
            handleBotModCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),                
                tokens);  
        }
        
        // -- Config
        if (tokens.get(0).equalsIgnoreCase("!config"))
        {
            handleConfigCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),                
                tokens);  
        }
        
        // -- Coins
        if (tokens.get(0).equalsIgnoreCase("!coins")
            || tokens.get(0).equalsIgnoreCase("!coin"))
        {
            handleCoinsCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),
                tokens);
        }
        
        // -- Gem
        if (tokens.get(0).equalsIgnoreCase("!gem"))
        {
            handleGemCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),
                tokens);
        }                
        
        // -- Dodge
        if (tokens.get(0).equalsIgnoreCase("!dodge"))
        {
            handleDodgeCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),
                tokens);
        }
        
        // -- Game
        if (tokens.get(0).equalsIgnoreCase("!game"))
        {
            handleGameCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),
                tokens);
        }
        
        // -- Roll
        if (tokens.get(0).equalsIgnoreCase("!roll"))
        {
            handleRollCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),
                tokens);
        }
        
        // -- BigPlay
        if (tokens.get(0).equalsIgnoreCase("!bigplay")
            || tokens.get(0).equalsIgnoreCase("!bigplays"))
        {
            handleBigPlayCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),                
                tokens);
        }
        
        // -- ShitList
        if (tokens.get(0).equalsIgnoreCase("!shitlist"))
        {
            handleShitListCommand(
                event.getMessage(),
                event.getUser(),
                tokens.size(),                
                tokens);  
        }
    }
    
    private void handleHelpCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Help command: " + message);
        
        // standalone
        if (numWords == 1)
        {
            helpPrint();
        }
        
        // set
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("set")
            && isAuthor(user))
        {
            helpSet(tokens.get(2));
        }
    }
    
    private void handleBotModCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** BotMod command: " + message);
        
        // add
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("add")
            && isAuthor(user))
        {
            botModAdd(tokens.get(2));
        }
        
        // delete
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("delete")
            && isAuthor(user))
        {
            botModRemove(tokens.get(2));
        }
    }
    
    private void handleConfigCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Config command: " + message);
        
        if (!isAuthor(user)) return;
        
        // set dodgedelay
        if (numWords >= 4 
            && tokens.get(1).equalsIgnoreCase("set")
            && tokens.get(2).equalsIgnoreCase("dodgedelay"))
        {
            configSetDodgeDelay(tokens.get(3));
        }
        
        // set gemdelay
        if (numWords >= 4 
            && tokens.get(1).equalsIgnoreCase("set")
            && tokens.get(2).equalsIgnoreCase("gemdelay"))
        {
            configSetGemDelay(tokens.get(3));
        }
        
        // set gamedelay
        if (numWords >= 4 
            && tokens.get(1).equalsIgnoreCase("set")
            && tokens.get(2).equalsIgnoreCase("gamedelay"))
        {
            configSetGameDelay(tokens.get(3));
        }
        
        // set rolldelay
        if (numWords >= 4 
            && tokens.get(1).equalsIgnoreCase("set")
            && tokens.get(2).equalsIgnoreCase("rolldelay"))
        {
            configSetRollDelay(tokens.get(3));
        }
    }
    
    private void handleCoinsCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Coins command: " + message);
        
        // (standalone)
        // balance
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("balance")
            || numWords == 1)
        {
            if (numWords == 1 || numWords == 2)
            {
                // Print the requestor's balance
                coinsPrintBalance(user.getNick());
            }
            else
            {
                // Print the specified user's balance.
                coinsPrintBalance(tokens.get(2));
            }
        }
        
        // give
        if (numWords >= 4 && tokens.get(1).equalsIgnoreCase("give"))
        {
            String giverNick = user.getNick();
            String receiverNick = tokens.get(2);
            String amountStr = tokens.get(3);
            
            coinsGive(giverNick, receiverNick, amountStr);
        }
        
        // leader
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("leader"))
        {
            coinsPrintLeaders(user.getNick());
        }
        
        // reset
        if (tokens.get(1).equalsIgnoreCase("reset")
            && isAuthor(user))
        {
            if (numWords > 3)
            {
                coinsReset(tokens.get(2), tokens.get(3));
            }
            else if (numWords == 3)
            {
                // Reset coins for specific user
                coinsReset(tokens.get(2));
            }
            else if (numWords == 2)
            {
                // Reset all Coins
                coinsReset();
            }
        }
    }
    
    private void handleGemCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Gem command: " + message);
        
        // begin
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("begin")
            && isModerator(user))
        {
            gemBegin();
        }
        
        // bet
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("bet"))
        {
            DateTime betDateTime = getDateTimeFromArgument(tokens.get(2));
            if (betDateTime != null)
            {
                gemBet(
                    user.getNick(),
                    betDateTime);
            }
        }
        
        // close
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("close")
            && isModerator(user))
        {
            gemClose();
        }
        
        // cancel
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("cancel")
            && isModerator(user))
        {
            gemCancel();
        }
        
        // end
        if (tokens.get(1).equalsIgnoreCase("end")
            && isModerator(user))
        {
            // If a game time is provided, end the contest with that result.
            if (numWords >= 3)
            {
                DateTime betDateTime = getDateTimeFromArgument(tokens.get(2));
                if (betDateTime != null)
                {
                    gemEnd(betDateTime);
                }
            }
            else 
            {
                // Cancel the contest.
                gemEnd();
            }
        }
    }
    
    private void handleDodgeCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Dodge command: " + message);
        
        // begin
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("begin")
            && isModerator(user))
        {
            dodgeBegin(mergeArguments(tokens, 2));
        }
        
        // bet
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("bet")
            && (tokens.get(2).equalsIgnoreCase("dodge")
                || tokens.get(2).equalsIgnoreCase("1v1")))
        {
            dodgeBet(
                user.getNick(),
                tokens.get(2).equalsIgnoreCase("dodge"));
        }
        
        // close
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("close")
            && isModerator(user))
        {
            dodgeClose();
        }
        
        // end
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("end")
            && (tokens.get(2).equalsIgnoreCase("dodge")
                || tokens.get(2).equalsIgnoreCase("1v1")))
        {
            dodgeEnd(tokens.get(2).equalsIgnoreCase("dodge"));
        }
    }
    
    private void handleGameCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Game command: " + message);
        
        // begin
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("begin")
            && isModerator(user))
        {
            gameBegin();
        }
        
        // bet
        if (numWords >= 4 && tokens.get(1).equalsIgnoreCase("bet"))
        {
            String winLoseStr = tokens.get(2);
            String amountStr = tokens.get(3);
            
            if (winLoseStr.equalsIgnoreCase("win")
                || winLoseStr.equalsIgnoreCase("lose"))
            {
                gameBet(
                    user.getNick(),
                    winLoseStr.equalsIgnoreCase("win"),
                    amountStr);
            }
        }
        
        // close
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("close")
            && isModerator(user))
        {
            gameClose();
        }
        
        // listbets
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("listbets"))
        {
            gameListBets();
        }
        
        // end
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("end")
            && isModerator(user))
        {
            String winLoseStr = tokens.get(2);
            if (winLoseStr.equalsIgnoreCase("win")
                || winLoseStr.equalsIgnoreCase("lose"))
            {
                gameEnd(winLoseStr.equalsIgnoreCase("win"));
            }
        }
        
        // cancel
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("cancel")
            && isModerator(user))
        {
            gameCancel();
        }
    }
    
    private void handleRollCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** Game command: " + message);
        
        // begin
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("begin"))
        {
            rollBegin(user.getNick(), tokens.get(2));
        }
        
        // bet
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("bet"))
        {
            // bet poor
            if (numWords >= 3 && tokens.get(2).equalsIgnoreCase("poor"))
            {
                rollPoorPlayerJoin(user.getNick());
            }
            else
            {
                rollBet(user.getNick());
            }
        }
        
        // end
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("end"))
        {
            rollEnd(user.getNick(), isModerator(user));
        }
    }
    
    private void handleBigPlayCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** BigPlay command: " + message);
        
        // get
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("get")
            || numWords == 1)
        {
            // Display the latest big play
            bigPlayPrintLatest(user.getNick());
        }

        // add
        else if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("add"))
        {
            // Register the big play
            bigPlayRegister(user.getNick(), mergeArguments(tokens, 2));
        }   
    }
            
    private void handleShitListCommand(
        String message,
        User user,
        int numWords,
        List<String> tokens)
    {
        System.out.println("\n** ShitList command: " + message);
        
        // add
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("add")
            && isModerator(user))
        {
            shitListAdd(tokens.get(2));
        }
        
        // delete
        if (numWords >= 3 && tokens.get(1).equalsIgnoreCase("delete")
            && isModerator(user))
        {
            shitListRemove(tokens.get(2));
        }
        
        // print
        if (numWords >= 2 && tokens.get(1).equalsIgnoreCase("print"))
        {
            shitListPrint();
        }
    }
    
    // Function(s)
    public void setPircBotX(PircBotX bot)
    {
        // Start message timer, if applicable
        if (messageTask_ == null)
        {
            messageTask_ = new MessageTask(bot.sendIRC());
            Timer messageTimer = new Timer(true);
            messageTimer.scheduleAtFixedRate(messageTask_, 0, MESSAGE_DELAY_MS);        
        }                
    }
    
    // -- Utility
    private void loadBackup()
    {                
        // Abort if backup file doesn't exist
        if (!new File(BACKUP_FILE).exists())
        {
            System.out.println("\n** Backup file doesn't exist. Not loading data.");
            return;
        }
        
        System.out.println("\n** Loading backup...");
        
        Properties prop = new Properties();
	InputStream input = null;
 
	try 
        {
            input = new FileInputStream(BACKUP_FILE);

            // Load the values
            prop.load(input);

            // Assign the values
            // -- Help
            if (prop.containsKey("help_url"))
            {
                System.out.println("(Help) parsing data...");
                helpUrl_ = prop.getProperty("help_url");
            }
            
            // -- BotMod
            if (prop.containsKey("bot_mod"))
            {
                String botModJsonStr = prop.getProperty("bot_mod");
                JSONArray botModJson = (JSONArray) JSONValue.parse(botModJsonStr);
                for (Object entry : botModJson)
                {
                    String nick = (String)entry;
                    botMod_.add(nick);
                    System.out.println("(BotMod) Adding nick: " + nick);
                }   
            } 
                        
            // -- Coins
            if (prop.containsKey("coins"))
            {
                String coinsJsonStr = prop.getProperty("coins");
                JSONObject coinsJson = (JSONObject) JSONValue.parse(coinsJsonStr);
                for (Map.Entry<String,Object> coinsEntry : coinsJson.entrySet()) 
                {
                    String key = coinsEntry.getKey();
                    Integer value = (Integer)coinsEntry.getValue();

                    coins_.put(key, value);
                    System.out.println("(Coins) Inserting key: " + key + ", value: " + value.toString());
                }
            }
            
            // -- Game Delays
            if (prop.containsKey("dodge_bet_period"))
                dodgeBetPeriod_ = 
                    Long.parseLong(prop.getProperty("dodge_bet_period"));
            
            if (prop.containsKey("gem_bet_period"))
                gemBetPeriod_ = 
                    Long.parseLong(prop.getProperty("gem_bet_period"));
            
            if (prop.containsKey("game_bet_period"))
                gameBetPeriod_ = 
                    Long.parseLong(prop.getProperty("game_bet_period"));
            
            if (prop.containsKey("roll_bet_period"))
                rollBetPeriod_ = 
                    Long.parseLong(prop.getProperty("roll_bet_period"));
                                    
            // -- BigPlay
            if (prop.containsKey("big_play_count")
                && prop.containsKey("latest_big_play_timestamp")
                && prop.containsKey("latest_big_play_nick")
                && prop.containsKey("latest_big_play_desc"))
            {
                System.out.println("(BigPlay) parsing data...");
                bigPlayCount_ = Integer.parseInt(prop.getProperty("big_play_count"));
                latestBigPlayTimestamp_ = Long.parseLong(prop.getProperty("latest_big_play_timestamp"));
                latestBigPlayNick_ = prop.getProperty("latest_big_play_nick");
                latestBigPlayDescription_ = prop.getProperty("latest_big_play_desc");
            }
            
            // -- ShitList
            if (prop.containsKey("shitlist"))
            {
                String shitListJsonStr = prop.getProperty("shitlist");
                JSONArray shitListJson = (JSONArray) JSONValue.parse(shitListJsonStr);
                for (Object entry : shitListJson)
                {
                    String nick = (String)entry;
                    shitList_.add(nick);
                    System.out.println("(ShitList) Adding nick: " + nick);
                }   
            } 
	} 
        catch (IOException ex) 
        {
            System.out.println("\n** Error while loading backup: " + ex.getMessage());
	} 
        finally 
        {
            // Close the stream
            if (input != null) 
            {
                try { input.close(); } 
                catch (IOException ex) { }
            }
            
            System.out.println("...done\n");
        }
    }
    
    public void saveBackup()
    {    
        System.out.println("\n** Saving backup...");
        
        // Open properties file
        Properties prop = new Properties();
	OutputStream output = null;
        try 
        {
            // Open file stream
            output = new FileOutputStream(BACKUP_FILE);
            
            // Save values
            // -- Help
            prop.setProperty("help_url", helpUrl_);
            
            // -- BotMod
            JSONArray botModJson = new JSONArray();
            botModJson.addAll(botMod_);
            String botModJsonStr = botModJson.toString();
            prop.setProperty("bot_mod", botModJsonStr);
            
            // -- Coins
            JSONObject coinsJson = new JSONObject();
            coinsJson.putAll(coins_);
            String coinsJsonStr = coinsJson.toString();
            prop.setProperty("coins", coinsJsonStr);
            
            // -- Game Delays
            prop.setProperty("dodge_bet_period",
                dodgeBetPeriod_.toString());

            prop.setProperty("gem_bet_period",
                gemBetPeriod_.toString());
            
            prop.setProperty("game_bet_period",
                gameBetPeriod_.toString());
            
            prop.setProperty("roll_bet_period",
                rollBetPeriod_.toString());
            
            // -- BigPlay
            prop.setProperty("big_play_count", bigPlayCount_.toString());
            prop.setProperty("latest_big_play_timestamp", Long.toString(latestBigPlayTimestamp_));
            prop.setProperty("latest_big_play_nick", latestBigPlayNick_);
            prop.setProperty("latest_big_play_desc", latestBigPlayDescription_);
        
            // -- ShitList
            JSONArray shitListJson = new JSONArray();
            shitListJson.addAll(shitList_);
            String shitListJsonStr = shitListJson.toString();
            prop.setProperty("shitlist", shitListJsonStr);
            
            // Save properties to project root folder
            prop.store(output, null);
	} 
        catch (IOException ex) 
        {
	    System.out.println("\n** Error while backing up: " + ex.getMessage());
	} 
        finally 
        {
            // Close the stream
            if (output != null) 
            {
                try { output.close(); } 
                catch (IOException ex) { }
            }
            
            System.out.println("...done\n");
        }
    }
    
    private void outputMessage(String message)
    {
        // Append the message to the timed queue
        messageTask_.addMessage(message);
    }
    
    private String mergeArguments(List<String> tokens, int argumentFirstIndex)
    {
        // Recreate the message, starting from the nth word (based on argumentFirstIndex)
        StringBuilder response = new StringBuilder();
        for (int i = argumentFirstIndex; i < tokens.size(); i++)
        {
            response.append(tokens.get(i));
            if (i < (tokens.size() - 1)) response.append(" ");
        }
        
        return response.toString();
    }
    
    private Boolean isAuthor(User user)
    {
        return user.getNick().equalsIgnoreCase(NICK_MASTER);
    }
    
    private Boolean isModerator(User user)
    {
        // Always true if author
        if (isAuthor(user)) return true;
        
        // Check if the user is a recognized bot mod
        if (botMod_.contains(user.getNick().toLowerCase()))
            return true;
        
        // Check if the user is a moderator in the channel
        for (Channel channel : user.getChannelsOpIn())
        {
            if (channel.getName().equalsIgnoreCase(IRC_CHANNEL))
                return true;
        }
        
        // No verification passed
        return false;
    }
    
    private DateTime getDateTimeFromArgument(String argument)
    {
        if (!argument.contains(":"))
            return null;
        
        if (argument.length() < 4)
            return null;
        
        Integer indexOfColumn = argument.indexOf(":");
        if (indexOfColumn == (argument.length() - 1))
            return null;
        
        String minsStr = argument.substring(0, indexOfColumn);
        String secondsStr = argument.substring(indexOfColumn + 1);
        
        try
        {
            int minutes = Integer.parseInt(minsStr);
            int seconds = Integer.parseInt(secondsStr);
            
            if (minutes >= 60 
                || minutes < 0
                || seconds >= 60
                || seconds < 0)
                return null;

            DateTime result = new DateTime(1970, 1, 1, 0, minutes, seconds);
            return result;
        }
        catch (NumberFormatException ex) { return null; }
    }
    
    // -- Help
    private void helpPrint()
    {
        if (!helpUrl_.isEmpty())
        {
            outputMessage("&diams;&diams; To see the list of commands, go to: " + helpUrl_);
        }
    }
    
    private void helpSet(String helpUrl)
    {
        helpUrl_ = helpUrl;
        outputMessage("&diams;&diams; Help URL stored.");
    }
    
    // -- BotMod
    private void botModAdd(String nick)
    {
        if (!botMod_.contains(nick.toLowerCase()))
        {
            botMod_.add(nick.toLowerCase());
            
            outputMessage("&diams;&diams; Added " + nick + " as a bot moderator.");
        }
    }
    
    private void botModRemove(String nick)
    {
        if (botMod_.contains(nick.toLowerCase()))
        {
            botMod_.remove(nick.toLowerCase());
            
            outputMessage("&diams;&diams; Removed " + nick + " as a bot moderator.");
        }
    }
    
    // -- Coins
    private void coinsUpdateBalance(String nick, Integer amount)
    {
        // Update the user's existing amount.
        // If the user doesn't have a stored amount, start
        // from the initial value.
        if (coins_.containsKey(nick.toLowerCase()))
        {
            coins_.put(
                nick.toLowerCase(),
                coins_.get(nick.toLowerCase()) + amount);
        }
        else
        {
            coins_.put(
                nick.toLowerCase(),
                COINS_INITIAL + amount);
        }
    }
    
    private Integer coinsGetBalance(String nick)
    {
        // Return the stored amount, otherwise return default
        if (coins_.containsKey(nick.toLowerCase()))
        {
            return coins_.get(nick.toLowerCase());
        }
        else return COINS_INITIAL;
    }
    
    private void coinsPrintBalance(String nick)
    {
        Integer balance = coinsGetBalance(nick);
        outputMessage("&diams;&diams; " + COINS_NAME + " balance for " + nick + ": " + balance + " " + COINS_NAME + ".");
    }
    
    private void coinsGive(
        String giverNick,
        String receiverNick,
        String amountStr)
    {
        // Ignore if giver and receiver is the same
        if (giverNick.equalsIgnoreCase(receiverNick))
            return;
        
        // Retrieve the giver's current balance
        Integer giverBalance = coinsGetBalance(giverNick);
        Integer amount;
            
        // Are we giving all?
        if (amountStr.equalsIgnoreCase("all"))
        {
            // Yes -- amount is equal to balance
            amount = giverBalance;
        }
        else
        {
            // No -- we are giving a specific amount.
            try
            {
                amount = Integer.parseInt(amountStr);
            }
            catch (NumberFormatException ex) { return; }
            
            // Is it bigger than what the user currently has?
            if (amount > giverBalance)
            {
                // Yes. Output error.
                outputMessage("&diams;&diams; Sorry " + giverNick + ", you are trying to give more than what you have! Amount: " + amount + " " + COINS_NAME + ", your balance: " + giverBalance + " " + COINS_NAME + ".");
                return;
            }
        }
        
        // Cancel if amount is zero
        if (amount <= 0) return;
        
        // Process the transaction.
        coinsUpdateBalance(receiverNick, amount);
        coinsUpdateBalance(giverNick, (-1 * amount));
        
        Integer receiverNewBalance = coinsGetBalance(receiverNick);
        Integer giverNewBalance = coinsGetBalance(giverNick);
        
        // Output message
        outputMessage("&diams;&diams; Transaction completed! " 
            + giverNick + " just gave " + amount + " " + COINS_NAME + " to " + receiverNick 
            + "! &#10026; New balance for " + receiverNick + ": " + receiverNewBalance + " " + COINS_NAME + "."
            + " &#10026; New balance for " + giverNick + ": " + giverNewBalance + " " + COINS_NAME + ".");
    }
    
    private Integer coinsGetLeaders(List<String> winnerNicks)
    {
        if (winnerNicks == null)
            winnerNicks = new ArrayList<>();

        // Search for the maximum value.
        Integer maxBalance = Integer.MIN_VALUE;
        for (String nick : coins_.keySet())
        {
            Integer balance = coinsGetBalance(nick);
            if (balance <= COINS_INITIAL) continue;
            
            if (balance == maxBalance)
            {
                // Tie in the balance.
                winnerNicks.add(nick);
            }
            else if (balance > maxBalance)
            {
                // New maximum reached
                maxBalance = balance;
                winnerNicks.clear();
                winnerNicks.add(nick);
            }            
        }
                
        return maxBalance;
    }
    
    private void coinsPrintLeaders(String requesterNick)
    {
        // Get the leader(s)
        List<String> currentWinnerNicks = new ArrayList<>();
        Integer maxBalance = coinsGetLeaders(currentWinnerNicks);
        
        // Do we have winners?
        if (!currentWinnerNicks.isEmpty())
        {
            if (currentWinnerNicks.size() == 1)
            {
                // One winner
                outputMessage("&diams;&diams; Current " + COINS_NAME + " leader is " + currentWinnerNicks.get(0) 
                    + " with " + maxBalance + " " + COINS_NAME + "!");
            }
            else
            {
                // Multiple winners
                StringBuilder winnerList = new StringBuilder();
                for (int i = 0; i < currentWinnerNicks.size(); i++)
                {
                    winnerList.append(currentWinnerNicks.get(i));
                    if (i != (currentWinnerNicks.size() - 1))
                        winnerList.append(", ");
                }
                
                outputMessage("&diams;&diams; Current " + COINS_NAME + " leaders are " + winnerList.toString()
                    + " with " + maxBalance + " " + COINS_NAME + "!");
            }
        }
        else
        {
            // No winners.
            outputMessage(
                "** Sorry " + requesterNick 
                + ", nobody currently has enough " + COINS_NAME + " to be at the top...");
        }
    }
    
    private void coinsReset()
    {
        coins_.clear();
        outputMessage("&diams;&diams; All " + COINS_NAME + " accounts reset "
            + "to default balance of " + COINS_INITIAL + " " + COINS_NAME + ".");
    }
    
    private void coinsReset(String nick)
    {
        if (coins_.containsKey(nick.toLowerCase()))
        {
            coins_.remove(nick.toLowerCase());
            
            outputMessage("&diams;&diams; " + COINS_NAME + " balance reset for " + nick + ".");
        }
    }
    
    private void coinsReset(String nick, String amountStr)
    {
        Integer amount;
        try { amount = Integer.parseInt(amountStr); }
        catch (NumberFormatException ex) { return; }
        
        // Overwrite the existing value
        coins_.put(nick.toLowerCase(), amount);
        outputMessage("&diams;&diams; " + COINS_NAME + " balance reset for " 
            + nick + ": " + amount + " " + COINS_NAME + ".");
    }
    
    // -- Config
    private void configSetDodgeDelay(String amountStr)
    {
        Long amount;
        try { amount = Long.parseLong(amountStr); }
        catch (NumberFormatException ex) { return; }
        
        dodgeBetPeriod_ = amount;
        outputMessage("&diams;&diams; Dodge Bet Period set to " + amount + "sec.");
    }
    
    private void configSetGameDelay(String amountStr)
    {
        Long amount;
        try { amount = Long.parseLong(amountStr); }
        catch (NumberFormatException ex) { return; }
        
        gameBetPeriod_ = amount;
        outputMessage("&diams;&diams; Game Bet Period set to " + amount + "sec.");        
    }
    
    private void configSetGemDelay(String amountStr)
    {
        Long amount;
        try { amount = Long.parseLong(amountStr); }
        catch (NumberFormatException ex) { return; }
        
        gemBetPeriod_ = amount;
        outputMessage("&diams;&diams; Gem Bet Period set to " + amount + "sec.");
    }
    
    private void configSetRollDelay(String amountStr)
    {
        Long amount;
        try { amount = Long.parseLong(amountStr); }
        catch (NumberFormatException ex) { return; }
        
        rollBetPeriod_ = amount;
        outputMessage("&diams;&diams; Roll Bet Period set to " + amount + "sec.");
    }
       
    // -- Gem
    private void gemBegin()
    {
        if (!gemEnabled_)
        {
            gemEnabled_ = true;
            gemBetsOpened_ = true;
            gemBets_.clear();
            gemBetTimer_ = new Timer();
            gemBetTimer_.schedule(new TimerTask()
            {
                @Override public void run() { gemClose(); }
            }, gemBetPeriod_ * 1000);
            
            outputMessage("&diams;&diams; The Gem bets begin! "
                + "&#10026; Guess the in-game time at which the gem will be lost. "
                + "&#10026; You have " + gemBetPeriod_ + "sec to join. "
                + "&#10026; To play: !gem bet [in-game time ##:##]");
        }
    }
    
    private void gemBet(String nick, DateTime gemTime)
    {
        if (gemEnabled_ && gemBetsOpened_)
        {
            // Check if user already put a bet
            if (gemBets_.containsKey(nick))
            {
                // Get current bet value
                DateTime currentBet = gemBets_.get(nick);
                        
                // Deny the new bet
                outputMessage("&diams;&diams; Sorry " + nick + ", you already placed a bet for [" + inGameTimeFormat_.format(currentBet.toDate()) + "]...");
            }
            else
            {            
                // Check if this time has already been selected
                for (String betNick : gemBets_.keySet())
                {
                    DateTime betTime = gemBets_.get(betNick);
                    if (betTime.equals(gemTime))
                    {
                        // Deny this bet: already betted on.
                        outputMessage("&diams;&diams; Sorry " + nick + ", user " + betNick + " has already bet on [" + inGameTimeFormat_.format(betTime.toDate()) + "]...");
                        return;
                    }
                }
                
                // Store the new bet
                gemBets_.put(nick, gemTime);
                
                // Output message
                Integer balance = coinsGetBalance(nick);
                outputMessage("&diams;&diams; User " + nick + " placed a bet for [" + inGameTimeFormat_.format(gemTime.toDate()) + "]! That player's current balance is: " + balance + " " + COINS_NAME + ".");
            }
        }
    }
    
    private void gemClose()
    {
        if (gemEnabled_
            && gemBetsOpened_)
        {
            // Disable betting
            gemBetsOpened_ = false;
            gemBetTimer_.cancel();
            
            outputMessage("&diams;&diams; Gem bets are now closed!");
        }
    }
    
    private void gemCancel()
    {
        if (gemEnabled_)
        {
            gemEnabled_ = false;
            gemBetTimer_.cancel();

            // Announce cancellation
            outputMessage("&diams;&diams; Gem contest cancelled. ");
        }
    }
    
    private void gemEnd()
    {
        if (gemEnabled_)
        {
            gemEnabled_ = false;
            gemBetTimer_.cancel();
            
            // Announce end of contest
            if (gemBets_.isEmpty())
            {
                // Nobody bet anything.
                outputMessage("&diams;&diams; Gem contest finished. No one bet anything.");
            }
            else
            {
                // When people vote against the streamer and he keeps the gem, he wins.
                coinsUpdateBalance(NICK_STREAMER, COINS_GEM_WIN);
                Integer streamerBalance = coinsGetBalance(NICK_STREAMER);
                outputMessage("&diams;&diams; Gem not lost!!! "
                    + NICK_STREAMER + " receives the " + COINS_GEM_WIN + " " + COINS_NAME + "! "
                    + "&#10026; Current balance for " + NICK_STREAMER + ": " + streamerBalance + " " + COINS_NAME + ".");
            }
        }
    }
    
    private void gemEnd(DateTime finalGemTime)
    {
        if (gemEnabled_)
        {
            gemEnabled_ = false;
            
            // Announce end of contest
            outputMessage("&diams;&diams; Gem bets are off! Computing results...");
        
            // Compute the winner.
            long minDifference = Long.MAX_VALUE;
            String currentWinnerNick = null;
            DateTime currentWinnerTime = null;
            
            for (String nick : gemBets_.keySet())
            {                
                DateTime betTime = gemBets_.get(nick);
                long difference = Math.abs(finalGemTime.getMillis() - betTime.getMillis());
                System.out.println("\n** Analyzing " + nick + " bet... difference = " + difference);
                
                if (difference < minDifference)                
                {
                    // We have a new potential winner.
                    System.out.println("\n** Current potential winner: " + nick);
                    minDifference = difference;
                    currentWinnerNick = nick;
                    currentWinnerTime = betTime;
                }
            }
            
            if (currentWinnerNick != null)
            {
                // We have a winner!
                coinsUpdateBalance(currentWinnerNick, COINS_GEM_WIN);
                Integer balance = coinsGetBalance(currentWinnerNick);
                
                outputMessage("&diams;&diams; Congratulations " + currentWinnerNick + "! You bet for [" + inGameTimeFormat_.format(currentWinnerTime.toDate()) + "], which was the closest to [" + inGameTimeFormat_.format(finalGemTime.toDate()) + "]! You win the Gem bets! You earned " + COINS_GEM_WIN + " " + COINS_NAME + " and your current balance is now " + balance + " " + COINS_NAME + ".");
            }
            else
            {
                // No winner (nobody bet?)
                outputMessage("&diams;&diams; No winner for the Gem bets... better luck next time!");
            }
        }
    }
    
    // -- Dodge
    private void dodgeBegin(String dodgeName)
    {
        if (!dodgeEnabled_)
        {
            dodgeEnabled_ = true;
            dodgeBetsOpened_ = true;
            dodgeName_ = dodgeName;
            dodgeBets_.clear();
            dodgeBetTimer_ = new Timer();
            dodgeBetTimer_.schedule(new TimerTask()
            {
                @Override public void run() { dodgeClose(); }
            }, dodgeBetPeriod_ * 1000);
            
            outputMessage("&diams;&diams; The Dodge bets begin! " 
                + "&#10026; " + dodgeName_ + " has been called out! Bet on whether he will dodge or 1v1. " 
                + "&#10026; You have " + dodgeBetPeriod_ + "sec to join. "
                + "&#10026; To play: !dodge bet [dodge/1v1]");
        }
    }

    private void dodgeBet(String nick, Boolean willDodge)
    {
        if (dodgeEnabled_
            && dodgeBetsOpened_)            
        {
            // Check if user already put a bet
            if (dodgeBets_.containsKey(nick))
            {
                // Get current bet value
                Boolean currentWillDodge = dodgeBets_.get(nick);
                        
                // Deny the new bet
                outputMessage("&diams;&diams; Sorry " + nick + ", you already placed a bet for [" 
                    + (currentWillDodge? "dodge" : "1v1") + "]...");
            }
            else
            {            
                // Store the new bet
                dodgeBets_.put(nick, willDodge);
                
                // Output message
                Integer balance = coinsGetBalance(nick);
                outputMessage("&diams;&diams; User " + nick + " placed a bet for [" + (willDodge? "dodge" : "1v1") + "]! That player's current balance is: " + balance + " " + COINS_NAME + ".");
            }
        }
    }
            
    private void dodgeClose()
    {
        if (dodgeEnabled_
            && dodgeBetsOpened_)
        {
            // Disable betting
            dodgeBetsOpened_ = false;
            dodgeBetTimer_.cancel();
            
            outputMessage("&diams;&diams; Dodge bets are now closed!");
        }
    }
    
    private void dodgeEnd(Boolean didDodge)
    {
        if (dodgeEnabled_)
        {
            dodgeEnabled_ = false;
            dodgeBetTimer_.cancel();
            
            // Compile list of winners
            List<String> winners = new ArrayList<>();
            for (String nick : dodgeBets_.keySet())
            {
                Boolean votedDodge = dodgeBets_.get(nick);
                if (votedDodge == didDodge)
                {
                    winners.add(nick);
                }
            }

            if (didDodge)
            {
                // Award those who bet for 'true' (will dodge)
                for (String nick : winners)
                {
                    coinsUpdateBalance(nick, COINS_DODGE_DODGE_WIN);
                }

                // Output message
                outputMessage("&diams;&diams; Dodge bets ended. No surprise there, " + dodgeName_ 
                    + " dodged like all of the other trash kids. "
                    + winners.size() + " winner(s) receive(s) " 
                    + COINS_DODGE_DODGE_WIN + " " + COINS_NAME + ". "                
                    + "&#10026; Check your balance with !coins");
            }
            else
            {
                // Award those who bet for 'false' (won't dodge)
                for (String nick : winners)
                {
                    coinsUpdateBalance(nick, COINS_DODGE_DUEL_WIN);
                }
                
                // Output message
                outputMessage("&diams;&diams; Dodge bets ended,  " + dodgeName_ 
                    + " agreed to do a 1v1! "
                    + winners.size() + " winner(s) receive(s) " 
                    + COINS_DODGE_DUEL_WIN + " " + COINS_NAME + "! "
                    + "&#10026; Check your balance with !coins");
            }
        }
    }
    
    // -- Game
    private Integer gameGetPoolSize()
    {
        Integer currentPoolSize = 0;
        for (Integer betAmount : gameBetsAmount_.values())
        {
            currentPoolSize += betAmount;
        }
        
        return currentPoolSize;
    }
    
    private void gameBegin()
    {
        if (!gameEnabled_)
        {
            gameEnabled_ = true;
            gameBetsOpened_ = true;
            gameBetsWinLose_.clear();
            gameBetsAmount_.clear();
            gameBetTimer_ = new Timer();
            gameBonusNick_ = null;
            gameBetTimer_.schedule(new TimerTask()
            {
                @Override public void run() { gameClose(); }
            }, gameBetPeriod_ * 1000);
                        
            outputMessage("&diams;&diams; The Game bets begin! " 
                + "Will " + NICK_STREAMER + " win or lose this game? "
                + "&#10026; You have " + gameBetPeriod_ + "sec to join. "
                + "&#10026; To play: !game bet [win/lose] [amount/all]");
        }
    }
    
    private void gameBet(String nick, Boolean willWin, String amountStr)
    {
        if (gameEnabled_
            && gameBetsOpened_)
        {
            // Did this player bet already?
            if (gameBetsWinLose_.containsKey(nick)
                && gameBetsAmount_.containsKey(nick))
            {
                // Yes. Retrieve information.
                Boolean betWin = gameBetsWinLose_.get(nick);
                Integer betAmount = gameBetsAmount_.get(nick);
                
                outputMessage("&diams;&diams; Sorry " + nick + ", you already bet on ["
                    + (betWin? "win" : "lose") + "] for this game, for "
                    + betAmount + " " + COINS_NAME + ".");
            }
            else
            {
                // Player hasn't bet yet.
                Integer playerCurrentBalance = coinsGetBalance(nick);
                Integer amount;
                if (amountStr.equalsIgnoreCase("all"))
                {
                    amount = playerCurrentBalance;
                }
                else
                {
                    try { amount = Integer.parseInt(amountStr); }
                    catch (NumberFormatException ex) { return; }
                }
                
                // Cancel if amount is zero.
                if (amount <= 0) return;
                
                // Cancel if amount is below threshold
                if (amount < COINS_POOR_THRESHOLD)
                {
                    outputMessage("&diams;&diams; Sorry " + nick + ", you need to bet at least "
                        + COINS_POOR_THRESHOLD + " " + COINS_NAME + " to participate.");                    
                    return;
                }
                                                
                // Does he have enough coins?
                if (playerCurrentBalance <= 0
                    || playerCurrentBalance < amount)
                {
                    // No.
                    outputMessage("&diams;&diams; Sorry " + nick + ", you don't have enough " + COINS_NAME + "! "
                        + "Amount: " + amount + " " + COINS_NAME + ", balance: " 
                        + playerCurrentBalance + " " + COINS_NAME + ".");
                }
                else
                {
                    // Player has enough coins.
                    // Store the bet.
                    gameBetsWinLose_.put(nick, willWin);
                    gameBetsAmount_.put(nick, amount);
                    
                    // Store bonus if applicable
                    String percentageAmount = null;                                            
                    if (gameBonusNick_ == null)
                    {
                        gameBonusNick_ = nick;
                        
                        percentageAmount = Integer.toString(
                            new BigDecimal(COINS_BONUS_MULTIPLIER)
                            .subtract(BigDecimal.ONE)
                            .multiply(new BigDecimal(100))
                            .round(new MathContext(0))
                            .intValue());
                    }                                        
                    
                    // Adjust balance
                    coinsUpdateBalance(nick, (-1 * amount));
                    Integer playerNewBalance = coinsGetBalance(nick);
                    
                    // Output message
                    outputMessage("&diams;&diams; User " + nick + " placed a bet for [" 
                        + (willWin? "win" : "lose") + "] for "
                        + amount + " " + COINS_NAME + "! "
                        + ((gameBonusNick_.equalsIgnoreCase(nick))? 
                            " Since " + nick + " bet first, he can get bonus earnings ("
                            + percentageAmount + "%)! "
                            : "")
                        + "That player's current balance "
                        + "(after putting money in the pool) is: " + playerNewBalance + " " + COINS_NAME + ". "
                        + "&#10026; Current pool size: " + gameGetPoolSize() + " " + COINS_NAME + ".");
                }                               
            }
        }
        else if (gameEnabled_ && !gameBetsOpened_)
        {
            outputMessage("&diams;&diams; Sorry " + nick 
                + ", bets are currently closed!");
        }
    }
    
    private void gameListBets()
    {
        if (gameEnabled_)
        {
            boolean commaWin = false;
            boolean commaLose = false;
            StringBuilder votersForWin = new StringBuilder();
            StringBuilder votersForLose = new StringBuilder();
            Integer winPoolSize = 0;
            Integer losePoolSize = 0;
            
            for (String nick : gameBetsWinLose_.keySet())
            {
                if (gameBetsWinLose_.get(nick))
                {
                    // Vote for win
                    if (commaWin) votersForWin.append(" &#10026; ");
                    votersForWin.append(nick);
                    votersForWin.append(" (");
                    votersForWin.append(gameBetsAmount_.get(nick));
                    votersForWin.append(")");
                    winPoolSize += gameBetsAmount_.get(nick);
                    
                    commaWin = true;
                }
                else
                {
                    // Vote for lose
                    if (commaLose) votersForLose.append(" &#10026; ");
                    votersForLose.append(nick);
                    votersForLose.append(" (");
                    votersForLose.append(gameBetsAmount_.get(nick));
                    votersForLose.append(")");
                    losePoolSize += gameBetsAmount_.get(nick);
                    
                    commaLose = true;
                }
            }
            
            outputMessage("&diams;&diams; Votes for [win] (" + winPoolSize + "): " 
                + votersForWin.toString());
            
            outputMessage("&diams;&diams; Votes for [lose] (" + losePoolSize + "): " 
                + votersForLose.toString());
        }
    }
    
    private void gameClose()
    {
        if (gameEnabled_
            && gameBetsOpened_)
        {
            // Disable betting
            gameBetsOpened_ = false;
            gameBetTimer_.cancel();
            
            outputMessage("&diams;&diams; Game bets are now closed! &#10026; Total pool size: " 
                + gameGetPoolSize() + " " + COINS_NAME + ".");
        }
    }
    
    private void gameEnd(Boolean didWin)
    {
        if (gameEnabled_)
        {
            gameEnabled_ = false;
            gameBetsOpened_ = false;
            gameBetTimer_.cancel();
            
            Integer totalPoolSize = gameGetPoolSize();
            
            // Abort if nobody bet
            if (totalPoolSize == 0)
            {
                outputMessage("&diams;&diams; Game ended! "
                    + "Unfortunately, nobody bet any " + COINS_NAME + ". Maybe next time!");
                
                return;
            }
            
            outputMessage("&diams;&diams; Game ended! The result is [" 
                + (didWin? "win" : "lose") + "]! "
                + "Total pool size: " + totalPoolSize + " " + COINS_NAME + "! "
                + "Computing results...");
            
            // Compute the winner's pool size
            Set<String> winners = new HashSet<>();
            Integer winnerPoolSize = 0;
            Integer loserPoolSize = 0;
            for (String nick : gameBetsWinLose_.keySet())
            {
                if (gameBetsWinLose_.get(nick).equals(didWin))
                {
                    winners.add(nick);
                    winnerPoolSize += gameBetsAmount_.get(nick);
                }
                else
                {
                    loserPoolSize += gameBetsAmount_.get(nick);
                }
            }
            
            // Abort early if everybody lost.
            if (winnerPoolSize == 0
                && loserPoolSize > 0)
            {
                coinsUpdateBalance(NICK_STREAMER, loserPoolSize);
                Integer streamerBalance = coinsGetBalance(NICK_STREAMER);
                
                outputMessage("&diams;&diams; Wow... everybody lost! "
                    + "All your " + COINS_NAME + " go to " + NICK_STREAMER + "!!! "
                    + "&#10026; Current balance for " + NICK_STREAMER + ": " + streamerBalance + " " + COINS_NAME + ".");
                
                return;
            }
            
            // Redistribute the money.
            Integer biggestGain = Integer.MIN_VALUE;
            List<String> biggestGainNicks = new ArrayList<>();
            for (String nick : winners)
            {
                // Deduce the gain
                Integer betAmount = gameBetsAmount_.get(nick);                
                Integer gain = 
                    Integer.valueOf(
                    new BigDecimal(betAmount)
                    .divide(new BigDecimal(winnerPoolSize), DIVISION_PRECISION, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(totalPoolSize))
                    .round(new MathContext(0)).intValue());
                
                // Bonus amount
                if (gameBonusNick_ != null
                    && gameBonusNick_.equalsIgnoreCase(nick))
                {
                    gain = new BigDecimal(gain).multiply(
                        new BigDecimal(COINS_BONUS_MULTIPLIER))
                        .round(new MathContext(0))
                        .intValue();
                }
                
                // Ensure a minimal gain
                if (gain.equals(betAmount))
                {
                    gain++;
                }
                
                // Update the balance                
                coinsUpdateBalance(nick, gain);
                
                if (gain.equals(biggestGain))
                {
                    // Add the winner to the list
                    biggestGainNicks.add(nick);
                }
                else if (gain > biggestGain)
                {
                    // Begin new record
                    biggestGain = gain;
                    biggestGainNicks.clear();
                    biggestGainNicks.add(nick);
                }
            }
            
            // Build list of biggest gainers
            StringBuilder biggestGainNicksList = new StringBuilder();
            for (int i = 0; i < biggestGainNicks.size(); i++)
            {
                biggestGainNicksList.append(biggestGainNicks.get(i));
                if (i != (biggestGainNicks.size() - 1))
                    biggestGainNicksList.append(", ");
            }
            
            // Output message
            if (loserPoolSize == 0)
            {
                // Everybody won
                outputMessage("&diams;&diams; Everybody who placed a bet won... all money returned. " 
                    + "&#10026; Check your balance with !coins");
            }
            else
            {
                // Not everybody won
                outputMessage("&diams;&diams; Congratulations to " + winners.size() + " winner(s)! "
                    + "&#10026; Biggest earner(s), with a gain of " + biggestGain + " " + COINS_NAME + ": "
                    + biggestGainNicksList.toString()
                    + " &#10026; Check your balance with !coins");
            }
        }
    }
    
    private void gameCancel()
    {
        if (gameEnabled_)
        {
            gameEnabled_ = false;
            gameBetsOpened_ = false;
            gameBetTimer_.cancel();
            
            // Refund all players
            for (String nick : gameBetsAmount_.keySet())
            {
                coinsUpdateBalance(
                    nick,
                    gameBetsAmount_.get(nick));
            }
            
            // Announce cancellation
            outputMessage("&diams;&diams; Game bets cancelled. "
                + "All bets refunded. "
                + "&#10026; Check your balance with !coins");
        }
    }
    
    // -- Roll
    private Integer rollGetPoolSize()
    {
        return (rollBetSize_ * rollPlayers_.size());
    }
    
    private void rollBegin(String initiatorNick, String initiatorBetStr)
    {
        if (!rollEnabled_)
        {
            Integer initiatorBet;
            if (initiatorBetStr.equalsIgnoreCase("all"))
            {
                // Retrieve the player's balance
                initiatorBet = coinsGetBalance(initiatorNick);
            }
            else
            {
                // Attempt to parse the Integer value.
                try { initiatorBet = Integer.parseInt(initiatorBetStr); }
                catch (NumberFormatException ex) { return; }
            }
            
            // Abort if bet size is too small.
            if (initiatorBet <= COINS_POOR_THRESHOLD)
            {
                outputMessage("&diams;&diams; Sorry " + initiatorNick + ", "
                    + "you need to initiate a roll contest with more than "
                    + COINS_POOR_THRESHOLD + " " + COINS_NAME + ".");
                
                return;
            }
            
            // Check if the initiator has the cash
            Integer initiatorBalance = coinsGetBalance(initiatorNick);
            if (initiatorBalance < initiatorBet)
            {
                outputMessage("&diams;&diams; Sorry " + initiatorNick + ", "
                    + "you don't have the " + initiatorBet + " " + COINS_NAME + " "
                    + "in your account (balance: " + initiatorBalance + " " + COINS_NAME + ").");
                
                return;
            }
            
            // Initiate game
            rollEnabled_ = true;          
            rollTimer_ = new Timer();
            rollTimer_.schedule(new TimerTask()
            {
                @Override public void run() { rollEnd(NICK_MASTER, true); }
            }, rollBetPeriod_ * 1000);
                            
            rollInitiatorNick_ = initiatorNick;
            rollBetSize_ = initiatorBet;
            rollPlayers_.clear();
            rollPoorPlayers_.clear();
            
            // Store the initiator's bet, deduct from balance            
            rollPlayers_.add(initiatorNick);
            coinsUpdateBalance(initiatorNick, (-1 * initiatorBet));
            
            outputMessage("&diams;&diams; A Roll betting contest has been launched: "
                + initiatorNick + " kicks it off, for " + initiatorBet + " " + COINS_NAME + "! "
                + "Rolls between 1-100, highest one wins! Pairs make poor players win! "
                + "&#10026; You have " + rollBetPeriod_ + "sec to join. "
                + "&#10026; To play (" + initiatorBet + " " + COINS_NAME + "): !roll bet "
                + "&#10026; Play as a poor player (free, must have less than " 
                + COINS_POOR_THRESHOLD + " " + COINS_NAME + "): !roll bet poor");
        }
    }
    
    private void rollBet(String nick)
    {
        if (rollEnabled_)
        {
            // If player is already a better, deny.
            if (rollPlayers_.contains(nick))
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you're already registered as a participant in this roll.");
                
                return;
            }
            
            // Verify player's balance
            Integer balance = coinsGetBalance(nick);
            if (balance < rollBetSize_)
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you don't have enough " + COINS_NAME + " (need " + rollBetSize_ + " " + COINS_NAME + ") "
                    + "to join this roll (your balance: " + balance + " " + COINS_NAME + ").");
                
                return;
            }
            
            // If player is already in the poor players pool, deny.
            if (rollPoorPlayers_.contains(nick))
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you're already registered as a poor player participant in this roll.");
                
                return;
            }
            
            // Add the player (deduct coins)
            rollPlayers_.add(nick);
            coinsUpdateBalance(nick, (-1 * rollBetSize_));
            Integer newBalance = coinsGetBalance(nick);
            
            outputMessage("&diams;&diams; " + nick + " "
                + " just added " + rollBetSize_ + " " + COINS_NAME + " to the pool and "
                + "has been added as a participant in this roll! "
                + "His remaining balance is: " + newBalance + " " + COINS_NAME + ". "
                + "&#10026; Total pool size: " + rollGetPoolSize() + " " + COINS_NAME + ".");
        }
    }
    
    private void rollPoorPlayerJoin(String nick)
    {
        if (rollEnabled_)
        {
            // If player is already a better, deny.
            if (rollPlayers_.contains(nick))
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you're already registered as a participant in this roll.");
                
                return;
            }
            
            // If player is above threshold, deny.
            Integer balance = coinsGetBalance(nick);
            if (balance > COINS_POOR_THRESHOLD)
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you have too many " + COINS_NAME + " (" + balance + ") "
                    + "to be eligible as a poor player in this roll.");
                
                return;
            }
            
            // If player is already in the poor players pool, deny.
            if (rollPoorPlayers_.contains(nick))
            {
                outputMessage("&diams;&diams; Sorry " + nick + ", "
                    + "you're already registered as a poor player participant in this roll.");
                
                return;
            }
            
            // Add the player
            rollPoorPlayers_.add(nick);
            
            outputMessage("&diams;&diams; " + nick + " "
                + "has been added as a poor player in this roll!");
        }
    }
    
    private void rollEnd(String requestorNick, Boolean requestorIsModerator)
    {
        if (rollEnabled_)
        {
            // Only proceed if the requestor is the initiator
            if (!requestorNick.equalsIgnoreCase(rollInitiatorNick_)
                && !requestorIsModerator)
            {
                outputMessage("&diams;&diams; Sorry " + requestorNick + ", "
                    + "only the initiator " + rollInitiatorNick_ 
                    + " (or a mod) can conclude this contest.");
                
                return;
            }
            
            // Disable game
            rollEnabled_ = false;
            rollTimer_.cancel();
            
            // Check how many participants we have.
            if (rollPlayers_.size() <= 1)
            {
                coinsUpdateBalance(
                    rollInitiatorNick_, 
                    rollBetSize_);
                
                outputMessage("&diams;&diams; Roll contest ended! "
                    + "Unfortunately, no players joined. "
                    + rollBetSize_ + " " + COINS_NAME + " refunded to " 
                    + rollInitiatorNick_ + ".");
                
                return;
            }
            
            // Start generating random numbers.
            Integer highestValue = Integer.MIN_VALUE;
            String currentWinner = null;
            
            Random rand = new Random();
            List<Integer> rollValues = new ArrayList<>();
            for (String nick : rollPlayers_)
            {
                // Roll a value (1 - 100)
                Integer currentValue = rand.nextInt(100) + 1;
                if (currentValue > highestValue)
                {
                    highestValue = currentValue;
                    currentWinner = nick;
                }
                
                // Output the roll value.
                if (currentValue == 1)
                {
                    outputMessage("&diams;&diams; " + nick 
                        + " rolled " + currentValue + " (lol get good)!");
                }
                else if (currentValue == 100)
                {
                    outputMessage("&diams;&diams; " + nick 
                        + " rolled " + currentValue + " (b-b-b-big play)!");
                }
                else
                {
                    outputMessage("&diams;&diams; " + nick 
                        + " rolled " + currentValue + "!");
                }
                                
                if (rollValues.contains(currentValue))
                {
                    // Pair!
                    // Do we have poor players?
                    if (rollPoorPlayers_.isEmpty())
                    {
                        // No. The money goes to the streamer.
                        coinsUpdateBalance(NICK_STREAMER, rollGetPoolSize());
                        Integer streamerBalance = coinsGetBalance(NICK_STREAMER);
                        
                        outputMessage("&diams;&diams; Oh shit! We have a pair! "
                            + "No poor players, the entire prize pool goes to " + NICK_STREAMER + "!!! "
                            + "Current balance for " + NICK_STREAMER + ": " + streamerBalance + " " + COINS_NAME + ".");
                    }
                    else
                    {
                        // Yes. The money is split.
                        Integer splitPoolAmount = Integer.valueOf(
                            new BigDecimal(rollGetPoolSize())
                            .divide(
                                new BigDecimal(rollPoorPlayers_.size()),
                                DIVISION_PRECISION,
                                RoundingMode.HALF_UP)
                            .round(new MathContext(0)).intValue());
                        
                        // Ensure a minimal gain
                        if (splitPoolAmount <= 0)
                            splitPoolAmount = 1;
                            
                        // Update each poor player's balance
                        for (String poorNick : rollPoorPlayers_)
                        {
                            coinsUpdateBalance(poorNick, splitPoolAmount);
                        }
                        
                        outputMessage("&diams;&diams; Oh shit! We have a pair! "
                            + "The entire prize pool is split amongst the "
                            + rollPoorPlayers_.size() + " poor player(s)! "
                            + "Each one receives " + splitPoolAmount + " " + COINS_NAME + "."
                            + "&#10026; Check your balance with !coins");                            
                    }
                    
                    return;
                }
                
                rollValues.add(currentValue);
            }
            
            // No pair encountered. Show the winner.
            if (currentWinner != null)
            {
                // Update balance
                coinsUpdateBalance(currentWinner, rollGetPoolSize());
                Integer newBalance = coinsGetBalance(currentWinner);
                
                outputMessage("&diams;&diams; Contest over! The winner is: "
                    + currentWinner + "! He takes in the entire prize pool of "
                    + rollGetPoolSize() + " " + COINS_NAME + "! "
                    + "His new balance is: " + newBalance + " " + COINS_NAME + ".");
            }
        }
    }
    
    // -- BigPlay
    private void bigPlayPrintLatest(String nick)
    {
        if (!latestBigPlayNick_.isEmpty()
            && !latestBigPlayDescription_.isEmpty())
        {
            // Output message
            outputMessage("&diams;&diams; Latest big play: " + latestBigPlayDescription_
                + " &#10026; submitted by " + latestBigPlayNick_ 
                + " [" + timestampFormat_.format(new Date(latestBigPlayTimestamp_)) + "] "
                + " &#10026; Total big plays: " + bigPlayCount_);
        }
        else
        {
            outputMessage("&diams;&diams; Sorry " + nick + ", no big plays registered yet...");
        }
    }
    
    private void bigPlayRegister(
        String nick,
        String description)
    {
        // Increment count
        bigPlayCount_++;
        
        // Store values
        latestBigPlayTimestamp_ = new Date().getTime();
        latestBigPlayNick_ = nick;
        latestBigPlayDescription_ = description;
                
        // Output message
        outputMessage("&diams;&diams; Big play registered! Thanks " + latestBigPlayNick_ + "!"
            + " &#10026; Total big plays: " + bigPlayCount_);
    }
    
    // -- ShitList
    private Boolean shitListContains(String nick)
    {
        return shitList_.contains(nick.toLowerCase());
    }
    
    private void shitListPrint()
    {
        boolean comma = false;
        StringBuilder shitlistPrint = new StringBuilder();
        for (String nick : shitList_)
        {
            if (comma) shitlistPrint.append(" &#10026; ");
            shitlistPrint.append(nick);
            
            comma = true;
        }
        
        outputMessage("&diams;&diams; Current shitlist: " + shitlistPrint.toString());
    }
    
    private void shitListAdd(String nick)
    {
        // Prevent adding the streamer or author
        if (nick.equalsIgnoreCase(NICK_STREAMER)
            || nick.equalsIgnoreCase(NICK_MASTER))
        {
            outputMessage("&diams;&diams; Nice try.");
            return;
        }       
        
        if (!shitListContains(nick))
        {
            shitList_.add(nick.toLowerCase());
            outputMessage("&diams;&diams; " + nick + " added to the shitlist.");
        }
        else
        {
            outputMessage("&diams;&diams; " + nick + " is already on the shitlist!");
        }
    }
    
    private void shitListRemove(String nick)
    {
        if (shitListContains(nick))
        {
            shitList_.remove(nick.toLowerCase());
            outputMessage("&diams;&diams; " + nick + " removed from the shitlist.");
        }
        else
        {
            outputMessage("&diams;&diams; Can't do, " + nick + " is not on the shitlist...");
        }
    }
    
    private void shitListGreet(String nick)
    {
        outputMessage("&diams;&diams; WARNING: trashcan shitkid " + nick.toUpperCase()
            + " has joined the stream. Get good kid, learn to fucking play.");
    }    
    
    // Message Queue
    private class MessageTask extends TimerTask
    {
        // Member(s)
        private final OutputIRC output_;
        private final Queue<String> messageQueue_;
        
        // Constructor(s)
        public MessageTask(OutputIRC output)
        {
            // Initialize member(s)
            output_ = output;
            messageQueue_ = new LinkedList<>();
        }
        
        // Function(s)
        public void addMessage(String message)
        {
            // Enqueue the message
            messageQueue_.add(message);
        }
        
        // Inheritance Implementation
        @Override
        public void run() 
        {
            // If message queue isn't empty...
            if (!messageQueue_.isEmpty())
            {
                // Process Message
                String message = messageQueue_.remove();
                output_.message(IRC_CHANNEL, message);
                System.out.println("\n** OUTPUT: " + message);
            }
        }
    }
    
    // Automated Backup
    private class BackupTask extends TimerTask
    {
        // Member(s)
        private final TwitchBot bot_;
        
        // Constructor(s)
        public BackupTask(TwitchBot bot)
        {
            // Initialize member(s)
            bot_ = bot;
        }
        
        // Inheritance Implementation
        @Override
        public void run() 
        {
            // Launch backup task
            bot_.saveBackup();
        }
    }
    
    // Main
    public static void main(String[] args)
    {
        // Instantiate bot module
        TwitchBot bot = new TwitchBot();
        
        // Setup IRC configuration
        Configuration configuration = new Configuration.Builder()
            .addListener(bot)
            .setName(IRC_NICKNAME)
            .setServerHostname(IRC_SERVER)
            .addAutoJoinChannel(IRC_CHANNEL)  
            .setServerPassword(IRC_PASSWORD)
            .setAutoReconnect(true)
            .buildConfiguration();

        // Connect to server & start process
        PircBotX pircBot = new PircBotX(configuration);
        bot.setPircBotX(pircBot);
        
        try 
        {
            pircBot.startBot();
        } 
        catch (IOException | IrcException ex) 
        {
            // Display error if connection failed.
            System.out.println("\n** Couldn't start bot: " + ex.getMessage() + "\n");
        }
    }
}
