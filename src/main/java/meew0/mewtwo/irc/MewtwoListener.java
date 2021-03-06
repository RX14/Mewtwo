package meew0.mewtwo.irc;

import meew0.mewtwo.MewtwoMain;
import meew0.mewtwo.commands.CommandChainBuilder;
import meew0.mewtwo.commands.ICommandChain;
import meew0.mewtwo.context.ContextManager;
import meew0.mewtwo.context.MewtwoContext;
import meew0.mewtwo.context.PermanentContext;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import java.util.Date;

public class MewtwoListener extends ListenerAdapter {
    private static final ContextManager ctxMgr = new ContextManager();

    public MewtwoListener() {
    }

    /**
     * Gets the static permanent context, to be used by threads such as FileWatchThread
     * @return permanent context of Mewtwo instance
     */
    public static PermanentContext getPCtx() {
        return ctxMgr.getPermanent();
    }

    void executeModules(String type, String msg, MewtwoContext ctx) {
        ctx.getPCtx().setThreadInfo(msg, ctx.getUserNick());
        try {
            String result = ctx.getPCtx().getModuleManager().executeModules(type, msg, ctx);

            if(result.length() > 500) {
                if(!ctx.getPCtx().isUserAdmin(ctx.getUser())) {
                    ctx.getChannel().send().message("Module result longer than the allowed character limit!");
                    return;
                }
            }

            String[] ret = result.split("\n");
            for (String s : ret) {
                ctx.getChannel().send().message(s);
            }
        } catch (Throwable t) {
            MewtwoMain.mewtwoLogger.error("Error while executing a module! (" + ctx.getId() + ")", t);
        }
    }

    @Override
    public void onMotd(MotdEvent event) throws Exception {
        MewtwoMain.mewtwoLogger.info("Successfully connected, now triggering connect- events");
        executeModules("connect", "", ctxMgr.makeContext(event.getBot(),
                new UserChannel(new ConsoleUser(event.getBot())), new ConsoleUser(event.getBot())));
        executeModules("connect-" + event.getBot().getServerInfo().getNetwork(), "",
                ctxMgr.makeContext(event.getBot(), new UserChannel(new ConsoleUser(event.getBot())),
                        new ConsoleUser(event.getBot())));
    }

    @Override
    public void onKick(KickEvent event) throws Exception {
        executeModules("kick", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser()));
    }

    @Override
    public void onInvite(InviteEvent event) throws Exception {
        executeModules("invite", "", ctxMgr.makeContext(event.getBot(),
                event.getBot().getUserChannelDao().getChannel(event.getChannel()),
                event.getBot().getUserChannelDao().getUser(event.getUser()))); // what the actual fuck PircBotX
    }

    @Override
    public void onOp(OpEvent event) throws Exception {
        if(event.isOp()) {
            executeModules("op", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getRecipient()));
        } else {
            executeModules("deop", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getRecipient()));
        }
    }

    @Override
    public void onVoice(VoiceEvent event) throws Exception {
        if(event.hasVoice()) {
            executeModules("voice", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getRecipient()));
        } else {
            executeModules("devoice", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getRecipient()));
        }
    }

    @Override
    public void onNotice(NoticeEvent event) throws Exception {
        if(event.getChannel() != null) {
            executeModules("notice", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser()));
        }
    }

    @Override
    public void onQuit(QuitEvent event) throws Exception {
        executeModules("quit", "", ctxMgr.makeContext(event.getBot(), new UserChannel(event.getUser()),
                event.getUser()));
    }

    @Override
    public void onPart(PartEvent event) throws Exception {
        executeModules("part", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser()));
    }

    @Override
    public void onJoin(JoinEvent event) throws Exception {
        executeModules("join", "", ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser()));
    }

    @Override
    public void onNickChange(NickChangeEvent event) throws Exception {
        executeModules("nickchange", "", ctxMgr.makeContext(event.getBot(), new UserChannel(event.getUser()),
                        event.getUser()));
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        MewtwoContext ctx = ctxMgr.makeContext(event.getBot(), new UserChannel(event.getUser()), event.getUser());
        executeOneChain(event.getMessage(), ctx);

        executeModules("pm", event.getMessage(), ctx);
    }

    private static void executeOneChain(String msg, MewtwoContext ctx) {
        ctx.getPCtx().setThreadInfo(msg, ctx.getUserNick());
        if (msg.startsWith(ctx.getPCtx().getMewtwoPrefix())) {
            CommandChainBuilder chainBuilder = new CommandChainBuilder(ctx, msg);
            ICommandChain chain = chainBuilder.buildChain();

            String result = chain.execute(ctx);
            ctx.benchmark("msg.cmd.chain.execute");

            if (result.length() <= 600 || ctx.getPCtx().isUserAdmin(ctx.getUser())) {
                // If the result is shorter than or equal to 500 characters,
                // post it to the channel
                String[] split = result.split("\n");
                if (split.length <= 4 || ctx.getPCtx().isUserAdmin(ctx.getUser())) {
                    for (String s : split) {
                        if (s.length() >= 445) {
                            String messageToSend = s.substring(0, 445);
                            if (!messageToSend.isEmpty())
                                ctx.getChannel().send().message(messageToSend);
                            s = s.substring(445, s.length());
                        }
                        if (!s.isEmpty()) ctx.getChannel().send().message(s);
                    }
                } else {
                    ctx.getChannel().send().message("Sorry, the response has exceeded the maximum amount of lines.");
                }
            } else {
                // Otherwise post the error message
                ctx.getChannel().send().message("Sorry, the response has exceeded the allowed character length.");
            }

            ctx.benchmark("msg.cmd.finish");

        }
    }

    @Override
    public void onAction(ActionEvent event) throws Exception {
        String msg = event.getMessage();
        MewtwoContext ctx = ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser());

        // Add message to log
        ctx.getPCtx().logMessage(msg, ctx.getUserNick());

        executeModules("action", event.getMessage(), ctx);
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        //new ThreadWatchThread(Thread.currentThread()).run();
        MewtwoContext ctx = ctxMgr.makeContext(event.getBot(), event.getChannel(), event.getUser());
        try {
            long counter = new Date().getTime();
            if (event.getUser().getNick().equals("Inumuta")) return;
            String msg = event.getMessage();

            ctx.getPCtx().logMessage(msg, ctx.getUserNick());
            ctx.benchmark("msg.pre");

            // Execute "message" module

            executeModules("message", msg, ctx);

            ctx.benchmark("msg.module");

            // Handle commands

            if (msg.startsWith(ctx.getPCtx().getMewtwoPrefix())) {
                executeOneChain(msg, ctx);
            }

            if(MewtwoMain.shouldBenchmark) {
                long diff = new Date().getTime() - counter;
                MewtwoMain.mewtwoLogger.info("Mewtwo took " + diff + " ms to execute!");
            }
        } catch (Throwable t) {
            // Error handling
            event.respond("ERROR: An exception has occurred! "
                    + t.getClass().getName() + ": " + t.getMessage());
            event.respond("See console for details.");
            MewtwoMain.mewtwoLogger.error("Error while processing message!", t);
        }
        if(MewtwoMain.shouldBenchmark) {
            MewtwoMain.mewtwoLogger.info("Benchmark data (times in ns): " + ctx.formatBenchmark());
        }
    }
}
