import React, { useState, useEffect } from 'react';
import Header from '../components/header';

export default function StudentView() {
    const [bookings, setBookings] = useState([]);
    const [selectedBooking, setSelectedBooking] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const studentEmail = localStorage.getItem('email');
    const jwt = localStorage.getItem('jwt');

    const approvalRoles = [
        'Faculty Head',
        'Student Council', 
        'Cultural Professor',
        'Floor Manager',
        'Security'
    ];

    // Add role mapping to backend enum values
    const roleToEnumMap = {
        'Faculty Head': 'FACULTY_HEAD',
        'Student Council': 'STUDENT_COUNCIL',
        'Cultural Professor': 'CULTURAL_PROF',
        'Floor Manager': 'FLOOR_MANAGER',
        'Security': 'SECURITY'
    };

    useEffect(() => {
        const fetchBookings = async () => {
            try {
                const response = await fetch(`http://localhost:8080/api/bookings?studentEmail=${studentEmail}`, {
                    headers: { Authorization: `Bearer ${jwt}` }
                });
                
                if (!response.ok) throw new Error('Failed to fetch bookings');
                
                const data = await response.json();
                setBookings(data);
                setLoading(false);
            } catch (err) {
                setError(err.message);
                setLoading(false);
            }
        };
        
        fetchBookings();
    }, [studentEmail, jwt]);

    const ApprovalStatusBadge = ({ status }) => (
        <span className={`status-badge ${status}`}>
            {status || 'Not Approved'}
        </span>
    );

    const ApprovalItem = ({ role, booking }) => {
        // Map display role to backend enum value
        const enumRole = roleToEnumMap[role];
        const approval = booking.approvals?.find(a => a.approverRole === enumRole);
        
        return (
            <div className="approval-item">
                <span className="role">{role}:</span>
                <ApprovalStatusBadge status={approval?.approvalStatus || 'Pending'} />
                {approval?.comments && (
                    <div className="comments">Comments: {approval.comments}</div>
                )}
            </div>
        );
    };

    const ApprovalModal = ({ booking, onClose }) => (
        <div className="approval-modal-overlay" onClick={onClose}>
            <div className="approval-modal" onClick={(e) => e.stopPropagation()}>
                <button className="close-button" onClick={onClose}>×</button>
                <h3>Approval Status for {booking.block}-{booking.roomNo}</h3>
                <p>Date: {new Date(booking.startTime).toLocaleDateString()}</p>
                <p>Club: {booking.clubName}</p>
                
                <div className="approval-list">
                    {approvalRoles.map(role => (
                        <ApprovalItem 
                            key={role}
                            role={role}
                            booking={booking}
                        />
                    ))}
                </div>
            </div>
        </div>
    );


    if (loading) return <div>Loading bookings...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <>
            <Header />
            <div className="student-view">
                <h2>Your Bookings</h2>
                <table className="bookings-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Time</th>
                            <th>Block</th>
                            <th>Room</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {bookings.map(booking => (
                            <tr key={booking.startTime} onClick={() => setSelectedBooking(booking)}>
                                <td>{new Date(booking.startTime).toLocaleDateString()}</td>
                                <td>
                                    {new Date(booking.startTime).toLocaleTimeString()} - 
                                    {new Date(booking.endTime).toLocaleTimeString()}
                                </td>
                                <td>{booking.block}</td>
                                <td>{booking.roomNo}</td>
                                <td>
                                    <ApprovalStatusBadge status={booking.overallStatus} />
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {selectedBooking && (
                    <ApprovalModal 
                        booking={selectedBooking} 
                        onClose={() => setSelectedBooking(null)}
                    />
                )}
            </div>
        </>
    );
}
