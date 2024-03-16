package io.github.yuokada.memcached.subcommand;

import io.github.yuokada.memcached.EntryCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "keys")
public class KeysCommand implements Callable<Integer> {

    /*
    if ($mode eq 'keys') {
        print STDERR "Dumping memcache keys";
        print STDERR " (limiting to $limit keys)" unless !$limit;
        print STDERR "\n";
        print $sock "lru_crawler metadump all\r\n";
        my %keyexp;
        my $keycount = 0;
        while (<$sock>) {
            last if /^END/ or ($limit and $keycount == $limit);
            # return format looks like this
            # key=foo exp=2147483647 la=1521046038 cas=717111 fetch=no cls=13 size=1232
            if (/^key=(\S+) exp=(-?\d+) .* /) {
                print $_
            }
            $keycount++;
        }
        exit;
    }
    */

    private static final Logger logger = LoggerFactory.getLogger(KeysCommand.class);
    @ParentCommand
    private EntryCommand entryCommand;

    @Option(
        names = {"--limit"}, description = "Number of keys to dump. 0 is no limit.",
        defaultValue = "0"
    )
    int limit;

    private static final String message = "Dumping memcache keys";

    @Override
    public Integer call() throws IOException {
        if (limit > 0) {
            message.concat(String.format(" (limiting to %d keys)", limit));
        }
        System.out.println(message);

        var serverAddress = entryCommand.configEndpoint;
        var serverPort = entryCommand.clusterPort;

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream());

            String command = "lru_crawler metadump all\r\n";
            writer.write(command);
            writer.flush();

            Pattern pattern = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

            var counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if (limit > 0 && counter >= limit) {
                    return ExitCode.OK;
                } else if (response.equals("END")) {
                    return ExitCode.OK;
                }

                if (pattern.matcher(response).matches()) {
                    System.out.println(response);
                    counter++;
                }
            }
            return ExitCode.OK;
        } catch (IOException e) {
            e.printStackTrace();
            return ExitCode.SOFTWARE;
        }
    }
}
