#include <iostream>
#include <istream>
#include <ostream>
#include <string>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/deadline_timer.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/write.hpp>

#include <android/log.h>

#define LOG(fmt, ...) __android_log_print(ANDROID_LOG_INFO, "TEST-BOOST", fmt, ##__VA_ARGS__)

using boost::asio::ip::tcp;
using boost::asio::deadline_timer;

class client
{
public:
    client(boost::asio::io_service& io_service, const std::string& data, const std::string& server, const unsigned short port)
            :   io_service_(io_service),
                socket_(io_service),
                heartbeat_timer_(io_service),
                deadline_(io_service),
                stop_(false)
    {
        LOG("[BOOST] client constructor: %s:%i", server.c_str(), port);

        data_ = data;
        tcp::endpoint end_point(boost::asio::ip::address::from_string(server), port);
        endpoint_ = end_point;
    }

    void start()
    {
        stop_ = false;
        deadline_.async_wait(boost::bind(&client::check_deadline, this));
        deadline_.expires_from_now(boost::posix_time::seconds(60));

        socket_.async_connect(endpoint_, boost::bind(&client::handle_connect, this, boost::asio::placeholders::error));
    }

    void stop()
    {
        if (stop_) {
            return;
        }
        LOG("[BOOST] client stop.");
        stop_ = true;
        socket_.close();
        deadline_.cancel();
        heartbeat_timer_.cancel();
        io_service_.stop();
    }

    std::string get_response()
    {
        return response_;
    }

private:
    void handle_connect(const boost::system::error_code& err)
    {
        if (!err)
        {
            LOG("[BOOST] client handle_connect connected. Sending data = %s", data_.c_str());
            start_write();
            start_read();
        }
        else
        {
            LOG("[BOOST] client handle_connect error = %s", err.message().c_str());
            stop();
        }
    }

    void start_read() {
        LOG("[BOOST] client start_read.");
        deadline_.expires_from_now(boost::posix_time::seconds(30));
        boost::asio::async_read_until(socket_, input_buffer_, "\n", boost::bind(&client::handle_read, this, _1));
    }


    void handle_read(const boost::system::error_code& err) {
        if (stop_) {
            return;
        }

        if (!err) {
            std::istream is(&input_buffer_);
            std::getline(is, response_);
            LOG("[BOOST] client handle_read server response: %s", response_.c_str());
            stop();
        } else {
            LOG("[BOOST] client handle_read error = %s", err.message().c_str());
            stop();
        }
    }

    void start_write() {
        boost::asio::async_write(socket_, boost::asio::buffer(data_), boost::bind(&client::handle_write, this, _1));
    }

    void handle_write(const boost::system::error_code& err) {
        if (stop_) {
            return;
        }

        if (!err) {
            LOG("[BOOST] client handle_write ");
            heartbeat_timer_.expires_from_now(boost::posix_time::seconds(10));
        } else {
            LOG("[BOOST] client handle_write error = %s", err.message().c_str());
            stop();
        }
    }

    void check_deadline()
    {
        if (stop_)
            return;

        // Check whether the deadline has passed. We compare the deadline against
        // the current time since a new asynchronous operation may have moved the
        // deadline before this actor had a chance to run.
        if (deadline_.expires_at() <= deadline_timer::traits_type::now())
        {
            LOG("[BOOST] check_deadline: TRUE. ");
            // The deadline has passed. The socket is closed so that any outstanding
            // asynchronous operations are cancelled.
            stop();

            // There is no longer an active deadline. The expiry is set to positive
            // infinity so that the actor takes no action until a new deadline is set.
            deadline_.expires_at(boost::posix_time::pos_infin);
        }

        LOG("[BOOST] check_deadline: FALSE. ");
        // Put the actor back to sleep.
        deadline_.async_wait(boost::bind(&client::check_deadline, this));
    }

    bool stop_;
    tcp::endpoint endpoint_;
    tcp::socket socket_;
    std::string data_;
    std::string response_;
    boost::asio::streambuf input_buffer_;
    deadline_timer deadline_;
    deadline_timer heartbeat_timer_;
    boost::asio::io_service& io_service_;
};