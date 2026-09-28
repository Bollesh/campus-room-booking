import React, { useEffect, useState } from 'react';
import Header from '../components/header';
import '../styles/index.css';

const FloorMgrDash = () => {
  const [bookings, setBookings] = useState([]);
  const [comments, setComments] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [managedBlocks, setManagedBlocks] = useState([]);

  const token = localStorage.getItem('jwt');
  const userEmail = localStorage.getItem('email');

  useEffect(() => {
    const fetchData = async () => {
      try {
        // First fetch rooms to determine managed blocks
        const roomsResponse = await fetch('http://localhost:8080/api/rooms', {
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (!roomsResponse.ok) throw new Error('Failed to load rooms');
        const roomsData = await roomsResponse.json();
        
        // Filter rooms managed by current floor manager
        const managerRooms = roomsData.filter(room => 
          room.manager?.email === userEmail
        );
        const blocks = [...new Set(managerRooms.map(room => room.block))];
        setManagedBlocks(blocks);

        // Now fetch all bookings
        const bookingsResponse = await fetch('http://localhost:8080/api/bookings', {
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (!bookingsResponse.ok) throw new Error('Failed to load bookings');
        const bookingsData = await bookingsResponse.json();

        // Filter bookings for managed blocks and not already approved/rejected
        const filteredBookings = bookingsData.filter(booking => 
          blocks.includes(booking.block) && 
          booking.overallStatus !== 'REJECTED' && // New rejection filter
          !booking.approvals?.some(approval => 
            approval.approverEmail === userEmail
          )
        );

        setBookings(filteredBookings);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [token, userEmail]);

  // Rest of the component remains unchanged
  const handleCommentChange = (bookingKey, value) => {
    setComments(prev => ({ ...prev, [bookingKey]: value }));
  };

  const handleApproval = async (booking, status) => {
    const bookingKey = `${booking.block}-${booking.roomNo}-${booking.startTime}`;
    const comment = comments[bookingKey] || '';
    try {
      const response = await fetch(
        `http://localhost:8080/api/bookings/${booking.block}/${booking.roomNo}/${booking.startTime}/approvals`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            approverEmail: userEmail,
            status: status,
            comments: comment
          })
        }
      );

      if (!response.ok) throw new Error('Approval action failed');
      
      // Remove approved booking from list
      setBookings(prev => 
        prev.filter(b => 
          !(b.block === booking.block && 
            b.roomNo === booking.roomNo && 
            b.startTime === booking.startTime)
        )
      );
      
      // Clear comment
      setComments(prev => {
        const newComments = { ...prev };
        delete newComments[bookingKey];
        return newComments;
      });
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;

  return (
    <>
      <Header />
      <div className="prof-dash-container">
        <h2>Pending Approvals - Managed Blocks: {managedBlocks.join(', ')}</h2>
        <table className="prof-dash-table">
          <thead>
            <tr>
              <th>Block</th>
              <th>Room</th>
              <th>Time</th>
              <th>Club</th>
              <th>Purpose</th>
              <th>Comments</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {bookings.map(booking => {
              const bookingKey = `${booking.block}-${booking.roomNo}-${booking.startTime}`;
              return (
                <tr key={bookingKey}>
                  <td>{booking.block}</td>
                  <td>{booking.roomNo}</td>
                  <td>{new Date(booking.startTime).toLocaleString()}</td>
                  <td>{booking.clubName}</td>
                  <td>{booking.purpose}</td>
                  <td>
                    <input
                      type="text"
                      placeholder="Add comments"
                      value={comments[bookingKey] || ''}
                      onChange={e => handleCommentChange(bookingKey, e.target.value)}
                      className="prof-dash-comment"
                    />
                  </td>
                  <td>
                    <button 
                      className="prof-dash-approve" 
                      onClick={() => handleApproval(booking, 'APPROVED')}
                    >
                      Approve
                    </button>
                    <button 
                      className="prof-dash-reject" 
                      onClick={() => handleApproval(booking, 'REJECTED')}
                    >
                      Reject
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default FloorMgrDash;
