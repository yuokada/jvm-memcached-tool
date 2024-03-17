package io.github.yuokada.memcached.subcommand;

import io.github.yuokada.memcached.EntryCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "dump")
public class DumpCommand implements Callable<Integer> {

    /*
     if ($mode eq 'dump') {
        print STDERR "Dumping memcache contents";
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
                my ($k, $exp) = ($1, $2);
                $k =~ s/%(.{2})/chr hex $1/eg;

                if ($exp == -1) {
                    $keyexp{$k} = 0;
                } else {
                    $keyexp{$k} = $exp;
                }
            }
            $keycount++;
        }

        if ($limit) {
            # Need to reopen the connection here to stop the metadump in
            # case the key limit was reached.
            #
            # XXX: Once a limit on # of keys returned is introduced in
            # `lru_crawler metadump`, this should be removed and the proper
            # parameter passed in the query above.
            close($sock);
            $sock = server_connect();
        }

        foreach my $k (keys(%keyexp)) {
            print $sock "get $k\r\n";
            my $response = <$sock>;
            if ($response =~ /VALUE (\S+) (\d+) (\d+)/) {
                my $flags = $2;
                my $len = $3;
                my $val;
                read $sock, $val, $len;
                print "add $k $flags $keyexp{$k} $len\r\n$val\r\n";
                # get the END
                $_ = <$sock>;
                $_ = <$sock>;
            }
        }
        exit;
    }
    */
    private static final Logger logger = LoggerFactory.getLogger(DumpCommand.class);
    @ParentCommand
    private EntryCommand entryCommand;

    @Option(
        names = {"--limit"}, description = "Number of keys to dump. 0 is no limit.",
        defaultValue = "0"
    )
    int limit;

    private static final String message = "Dumping memcache contents";

    @Override
    public Integer call() {
        if (limit < 0) {
            System.err.println("Limit must be greater than or equal to 0");
            return ExitCode.USAGE;
        }

        if (limit > 0) {
            System.err.println(message.concat(String.format(" (limiting to %d keys)", limit)));
        } else {
            System.err.println(message);
        }

        var serverAddress = entryCommand.configEndpoint;
        var serverPort = entryCommand.clusterPort;
        var client = entryCommand.memcachedClient;

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream());

            String command = "lru_crawler metadump all\r\n";
            writer.write(command);
            writer.flush();

            Pattern pattern = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

            var counter = 0;
            String response;
            List<DumpObject> list = new ArrayList<>();

            while ((response = reader.readLine()) != null) {
                if (limit > 0 && counter >= limit || response.equals("END")) {
                    break;
                }

                Matcher matcher = pattern.matcher(response);
                if (matcher.matches()) {
                    var key = matcher.group(1);
                    var expiration = Integer.parseInt(matcher.group(2));

                    String vResponse = (String) client.get(key);
                    // GetFuture<Object> objectGetFuture = client.asyncGet(key);
                    list.add(new DumpObject(key, expiration, response, vResponse));
                    counter++;
                }
            }

            list.forEach(e -> {
                String fmt = String.format("add %s 0 %d %d\n%s", e.key, e.expiration,
                    e.value.length(), e.value);
                System.out.println(fmt);
            });

            return ExitCode.OK;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }

    static class DumpObject {

        String key;
        Integer expiration;
        String expression;
        String value;

        public DumpObject(String key, Integer expiration, String expression, String value) {
            this.key = key;
            this.expiration = expiration;
            this.expression = expression;
            this.value = value;
        }

        @Override
        public String toString() {
            return "DumpObject{" +
                "key='" + key + '\'' +
                ", expiration=" + expiration +
                ", expression='" + expression + '\'' +
                ", value='" + value + '\'' +
                '}';
        }
    }
}
