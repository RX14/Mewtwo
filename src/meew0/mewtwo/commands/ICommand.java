package meew0.mewtwo.commands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

public interface ICommand {
	public String getCommandName();
	public String getAlias();
	public void onExecution(String[] args, MessageEvent<PircBotX> event);
	public String getHelpEntry();
}